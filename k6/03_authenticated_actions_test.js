/**
 * K6 Load Test #3 — Authenticated User Actions (Posts, Likes, Comments)
 * ======================================================================
 * Tests: POST /api/v1/posts, GET /api/v1/posts, POST /api/v1/posts/:id/like,
 *        POST /api/v1/comments, GET /api/v1/users/profile
 *
 * WHY TEST THIRD:
 * - These are write-heavy endpoints → highest DB write pressure
 * - Concurrent writes can cause deadlocks and race conditions
 * - Tests JWT verification overhead under load
 *
 * SETUP: Set TOKEN env var with a valid JWT
 * Run: k6 run -e BASE_URL=https://campus-sync-9luf.onrender.com -e TOKEN=<jwt> 03_authenticated_actions_test.js
 */
import http from "k6/http";
import { check, sleep, group } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";

// ─── Custom Metrics ────────────────────────────────────────────────────────
const writeSuccessRate = new Rate("write_success_rate");
const readSuccessRate = new Rate("read_success_rate");
const writeDuration = new Trend("write_duration_ms", true);
const readDuration = new Trend("auth_read_duration_ms", true);
const writeErrors = new Counter("write_errors");

// ─── Config ────────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "https://campus-sync-fpqp.onrender.com";
const TOKEN = __ENV.TOKEN;

if (!TOKEN) {
  console.error("ERROR: TOKEN env var is required. Run: k6 run -e TOKEN=<your_jwt> 03_authenticated_actions_test.js");
}

// ─── Load Stages ───────────────────────────────────────────────────────────
export const options = {
  stages: [
    { duration: "30s", target: 5  },  // Warm up
    { duration: "1m",  target: 20 },  // Ramp up
    { duration: "2m",  target: 30 },  // Sustain
    { duration: "30s", target: 50 },  // Spike
    { duration: "30s", target: 0  },  // Ramp down
  ],
  thresholds: {
    http_req_duration:  ["p(95)<4000"],  // Writes are slower
    write_success_rate: ["rate>0.90"],   // 90%+ writes succeed
    read_success_rate:  ["rate>0.95"],
    write_duration_ms:  ["p(95)<3000"],
    http_req_failed:    ["rate<0.10"],
  },
};

// ─── Headers ───────────────────────────────────────────────────────────────
const authHeaders = {
  "Content-Type": "application/json",
  Authorization: `Bearer ${TOKEN}`,
};

// ─── Sample post content ───────────────────────────────────────────────────
const POST_CONTENTS = [
  "Just finished an amazing workshop on machine learning! 🤖",
  "The campus hackathon was incredible - built something amazing in 24 hours!",
  "Looking for teammates for the upcoming coding competition. Drop a comment!",
  "Reminder: Club meeting tomorrow at 5 PM in the CS lab.",
  "Great lecture today on distributed systems. Notes in the drive!",
];

function randomContent() {
  const ts = Date.now();
  const base = POST_CONTENTS[Math.floor(Math.random() * POST_CONTENTS.length)];
  return `${base} [Load Test - ${ts}]`;
}

// ─── Main Test ─────────────────────────────────────────────────────────────
export default function () {
  if (!TOKEN) { sleep(1); return; }

  let createdPostId = null;

  group("Profile Read", () => {
    // ── Test 1: Get own profile ───────────────────────────────────────────
    const readStart = Date.now();
    const profileRes = http.get(
      `${BASE_URL}/api/v1/users/profile`,
      { headers: authHeaders, tags: { name: "profile_me" } }
    );
    readDuration.add(Date.now() - readStart);

    const profileOk = check(profileRes, {
      "profile: status 200":             (r) => r.status === 200,
      "profile: has name":               (r) => !!r.json("name"),
      "profile: response time < 1500ms": (r) => r.timings.duration < 1500,
    });
    readSuccessRate.add(profileOk);

    sleep(0.5);
  });

  group("Post Write", () => {
    // ── Test 2: Create a post ─────────────────────────────────────────────
    const writeStart = Date.now();
    const createRes = http.post(
      `${BASE_URL}/api/v1/posts`,
      JSON.stringify({ content: randomContent() }),
      { headers: authHeaders, tags: { name: "post_create" } }
    );
    writeDuration.add(Date.now() - writeStart);

    const createOk = check(createRes, {
      "create post: status 200 or 201":   (r) => r.status === 200 || r.status === 201,
      "create post: has id":              (r) => !!r.json("id"),
      "create post: response time < 3000ms": (r) => r.timings.duration < 3000,
    });
    writeSuccessRate.add(createOk);

    if (!createOk) {
      writeErrors.add(1);
      console.error(`Post create failed [${createRes.status}]: ${createRes.body?.substring(0, 200)}`);
    } else {
      createdPostId = createRes.json("id");
    }

    sleep(0.5);
  });

  group("Post Read", () => {
    // ── Test 3: List all posts ────────────────────────────────────────────
    const postsStart = Date.now();
    const postsRes = http.get(
      `${BASE_URL}/api/v1/posts?page=0&size=10`,
      { headers: authHeaders, tags: { name: "posts_list" } }
    );
    readDuration.add(Date.now() - postsStart);

    const postsOk = check(postsRes, {
      "posts list: status 200":            (r) => r.status === 200,
      "posts list: response time < 2000ms": (r) => r.timings.duration < 2000,
    });
    readSuccessRate.add(postsOk);

    // Extract an existing post ID for like/comment test
    let targetPostId = createdPostId;
    if (!targetPostId && postsOk) {
      try {
        const posts = postsRes.json();
        const content = Array.isArray(posts.content) ? posts.content : posts;
        if (content.length > 0) targetPostId = content[0].id;
      } catch (_) {}
    }

    sleep(0.5);

    if (targetPostId) {
      group("Engagement Actions", () => {
        // ── Test 4: Like a post ─────────────────────────────────────────
        const likeRes = http.post(
          `${BASE_URL}/api/v1/posts/${targetPostId}/like`,
          null,
          { headers: authHeaders, tags: { name: "post_like" } }
        );
        const likeOk = check(likeRes, {
          "like: status 200 or 201 or 409": (r) => [200, 201, 409].includes(r.status), // 409 = already liked
          "like: response time < 1500ms":   (r) => r.timings.duration < 1500,
        });
        writeSuccessRate.add(likeOk);

        sleep(0.3);

        // ── Test 5: Add a comment ───────────────────────────────────────
        const commentRes = http.post(
          `${BASE_URL}/api/v1/comments`,
          JSON.stringify({ postId: targetPostId, content: `Great post! [k6-${Date.now()}]` }),
          { headers: authHeaders, tags: { name: "comment_create" } }
        );
        const commentOk = check(commentRes, {
          "comment: status 200 or 201":    (r) => r.status === 200 || r.status === 201,
          "comment: response time < 2000ms": (r) => r.timings.duration < 2000,
        });
        writeSuccessRate.add(commentOk);
        if (!commentOk) writeErrors.add(1);
      });
    }
  });

  group("User Discovery", () => {
    // ── Test 6: Search (authenticated) ────────────────────────────────────
    const searchRes = http.get(
      `${BASE_URL}/api/v1/events/search?keyword=tech&page=0&size=5`,
      { headers: authHeaders, tags: { name: "events_search_auth" } }
    );
    check(searchRes, {
      "search auth: status 200":            (r) => r.status === 200,
      "search auth: response time < 2000ms": (r) => r.timings.duration < 2000,
    });
  });

  sleep(Math.random() * 3 + 2); // 2–5s realistic think time
}

// ─── Summary ───────────────────────────────────────────────────────────────
export function handleSummary(data) {
  console.log("=== AUTHENTICATED ACTIONS LOAD TEST SUMMARY ===");
  console.log(`Write Success Rate: ${(data.metrics.write_success_rate?.values?.rate * 100).toFixed(1)}%`);
  console.log(`Read Success Rate:  ${(data.metrics.read_success_rate?.values?.rate * 100).toFixed(1)}%`);
  console.log(`Write Errors:       ${data.metrics.write_errors?.values?.count ?? 0}`);
  console.log(`Avg Write Duration: ${data.metrics.write_duration_ms?.values?.avg?.toFixed(0)}ms`);
  console.log(`P95 Write Duration: ${data.metrics.write_duration_ms?.values?.["p(95)"]?.toFixed(0)}ms`);
  return {};
}
