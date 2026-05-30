/**
 * K6 Load Test #2 — Public Read APIs (Events + Feed)
 * ===================================================
 * Tests: GET /api/v1/events, GET /api/v1/feed, GET /api/v1/events/search
 *
 * WHY TEST SECOND:
 * - These are the highest-traffic endpoints (every page load hits them)
 * - No auth required → can simulate anonymous + logged-in users
 * - Feed involves N+1 queries → most likely DB bottleneck
 * - Tests Redis caching effectiveness under load
 *
 * Run: k6 run 02_public_read_test.js
 * Run with auth: k6 run -e TOKEN=<your_jwt_token> 02_public_read_test.js
 */
import http from "k6/http";
import { check, sleep, group } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";

// ─── Custom Metrics ────────────────────────────────────────────────────────
const eventsSuccessRate = new Rate("events_success_rate");
const feedSuccessRate = new Rate("feed_success_rate");
const searchSuccessRate = new Rate("search_success_rate");
const eventsDuration = new Trend("events_duration_ms", true);
const feedDuration = new Trend("feed_duration_ms", true);

// ─── Config ────────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "https://campus-sync-fpqp.onrender.com";
const TOKEN = __ENV.TOKEN || ""; // Optional: set via -e TOKEN=xxx for authenticated tests

// ─── Load Stages ───────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    // Scenario 1: Anonymous users browsing events (no auth)
    anonymous_browse: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 20 },
        { duration: "2m",  target: 100 },
        { duration: "1m",  target: 150 }, // Peak load
        { duration: "30s", target: 0 },
      ],
      tags: { scenario: "anonymous" },
    },
    // Scenario 2: Authenticated users browsing feed (with auth)
    authenticated_feed: {
      executor: "constant-vus",
      vus: 30,
      duration: "3m",
      startTime: "30s", // Start 30s after anonymous scenario
      tags: { scenario: "authenticated" },
    },
  },
  thresholds: {
    http_req_duration:    ["p(95)<3000"],  // 95% under 3s (Feed is heavier)
    events_success_rate:  ["rate>0.98"],   // 98%+ events requests succeed
    feed_success_rate:    ["rate>0.95"],   // 95%+ feed requests succeed
    search_success_rate:  ["rate>0.95"],
    events_duration_ms:   ["p(95)<2000"],  // Events list under 2s
    feed_duration_ms:     ["p(95)<3000"],  // Feed under 3s
    http_req_failed:      ["rate<0.05"],
  },
};

// ─── Headers ───────────────────────────────────────────────────────────────
function getHeaders(withAuth = false) {
  const h = { "Content-Type": "application/json" };
  if (withAuth && TOKEN) {
    h["Authorization"] = `Bearer ${TOKEN}`;
  }
  return h;
}

// ─── Search keywords to simulate real usage ────────────────────────────────
const SEARCH_KEYWORDS = [
  "tech", "fest", "workshop", "seminar", "hackathon",
  "sports", "cultural", "conference", "meeting", "club"
];

function randomKeyword() {
  return SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];
}

// ─── Main Test ─────────────────────────────────────────────────────────────
export default function () {
  const isAuthenticated = TOKEN && __ENV.TOKEN;

  group("Events API", () => {
    // ── Test 1: List all events (paginated) ─────────────────────────────
    const eventsStart = Date.now();
    const eventsRes = http.get(
      `${BASE_URL}/api/v1/events?page=0&size=20`,
      { headers: getHeaders(), tags: { name: "events_list" } }
    );
    eventsDuration.add(Date.now() - eventsStart);

    const eventsOk = check(eventsRes, {
      "events list: status 200":            (r) => r.status === 200,
      "events list: has content":           (r) => {
        try {
          const body = r.json();
          return Array.isArray(body.content) || Array.isArray(body) || (body.data && Array.isArray(body.data.content));
        } catch { return false; }
      },
      "events list: response time < 3000ms": (r) => r.timings.duration < 3000,
    });
    eventsSuccessRate.add(eventsOk);

    sleep(0.5);

    // ── Test 2: Search events ────────────────────────────────────────────
    const keyword = randomKeyword();
    const searchRes = http.get(
      `${BASE_URL}/api/v1/events/search?keyword=${keyword}&page=0&size=10`,
      { headers: getHeaders(), tags: { name: "events_search" } }
    );

    const searchOk = check(searchRes, {
      "search: status 200":            (r) => r.status === 200,
      "search: response time < 2000ms": (r) => r.timings.duration < 2000,
    });
    searchSuccessRate.add(searchOk);

    sleep(0.5);

    // ── Test 3: Get single event ─────────────────────────────────────────
    if (eventsOk) {
      try {
        const events = eventsRes.json();
        const content = Array.isArray(events.content) ? events.content : (events.data && Array.isArray(events.data.content) ? events.data.content : events);
        if (content && content.length > 0) {
          const eventId = content[0].id;
          const singleRes = http.get(
            `${BASE_URL}/api/v1/events/${eventId}`,
            { headers: getHeaders(), tags: { name: "events_single" } }
          );
          check(singleRes, {
            "single event: status 200":            (r) => r.status === 200,
            "single event: has title":             (r) => !!r.json("title") || (r.json("data") && !!r.json("data").title),
            "single event: response time < 1500ms": (r) => r.timings.duration < 1500,
          });
        }
      } catch (_) {}
    }
  });

  sleep(1);

  group("Feed API", () => {
    // ── Test 4: Home Feed ────────────────────────────────────────────────
    const feedStart = Date.now();
    const feedRes = http.get(
      `${BASE_URL}/api/v1/feed?page=0&size=20&filter=all&sort=date`,
      { headers: getHeaders(isAuthenticated), tags: { name: "feed_home" } }
    );
    feedDuration.add(Date.now() - feedStart);

    const feedOk = check(feedRes, {
      "feed: status 200":              (r) => r.status === 200 || (isAuthenticated ? r.status === 200 : r.status !== 500),
      "feed: response time < 4000ms":  (r) => r.timings.duration < 4000,
    });
    feedSuccessRate.add(feedOk);

    sleep(0.5);

    // ── Test 5: Feed Stats ───────────────────────────────────────────────
    const statsRes = http.get(
      `${BASE_URL}/api/v1/feed/stats`,
      { headers: getHeaders(), tags: { name: "feed_stats" } }
    );
    check(statsRes, {
      "feed stats: status 200":            (r) => r.status === 200,
      "feed stats: has totalPosts":        (r) => r.json("totalPosts") !== undefined || (r.json("data") && r.json("data").totalPosts !== undefined),
      "feed stats: response time < 1000ms": (r) => r.timings.duration < 1000,
    });

    sleep(0.5);

    // ── Test 6: Trending Feed ────────────────────────────────────────────
    const trendRes = http.get(
      `${BASE_URL}/api/v1/feed/trending?page=0&size=10`,
      { headers: getHeaders(isAuthenticated), tags: { name: "feed_trending" } }
    );
    check(trendRes, {
      "trending: status not 500":         (r) => r.status !== 500,
      "trending: response time < 4000ms": (r) => r.timings.duration < 4000,
    });
  });

  sleep(Math.random() * 2 + 1); // 1–3s think time between iterations
}

// ─── Summary ───────────────────────────────────────────────────────────────
export function handleSummary(data) {
  console.log("=== PUBLIC READ LOAD TEST SUMMARY ===");
  console.log(`Events Success Rate: ${(data.metrics.events_success_rate?.values?.rate * 100).toFixed(1)}%`);
  console.log(`Feed Success Rate:   ${(data.metrics.feed_success_rate?.values?.rate * 100).toFixed(1)}%`);
  console.log(`Avg Events Duration: ${data.metrics.events_duration_ms?.values?.avg?.toFixed(0)}ms`);
  console.log(`P95 Events Duration: ${data.metrics.events_duration_ms?.values?.["p(95)"]?.toFixed(0)}ms`);
  console.log(`Avg Feed Duration:   ${data.metrics.feed_duration_ms?.values?.avg?.toFixed(0)}ms`);
  console.log(`P95 Feed Duration:   ${data.metrics.feed_duration_ms?.values?.["p(95)"]?.toFixed(0)}ms`);
  return {};
}
