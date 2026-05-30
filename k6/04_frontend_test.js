/**
 * K6 Load Test #4 — Frontend SPA
 * ===================================================
 * Tests static file delivery of the deployed frontend.
 *
 * Run: k6 run -e FRONTEND_URL=<url> 04_frontend_test.js
 */
import http from "k6/http";
import { check, sleep, group } from "k6";
import { Rate, Trend } from "k6/metrics";

// ─── Custom Metrics ────────────────────────────────────────────────────────
const frontendSuccessRate = new Rate("frontend_success_rate");
const frontendDuration = new Trend("frontend_duration_ms", true);

// ─── Config ────────────────────────────────────────────────────────────────
const FRONTEND_URL = __ENV.FRONTEND_URL || "http://localhost:5173";

// ─── Load Stages ───────────────────────────────────────────────────────────
export const options = {
  scenarios: {
    // Simulate real users loading the SPA
    spa_load: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: 10 },
        { duration: "40s",  target: 20 }, // Lower peak concurrent users to avoid triggering Vercel rate limits
        { duration: "20s", target: 0 },
      ],
      tags: { scenario: "frontend" },
    },
  },
  thresholds: {
    http_req_duration:      ["p(95)<1500"], // Static assets should be fast
    frontend_success_rate:  ["rate>0.99"],  // 99%+ requests succeed
    frontend_duration_ms:   ["p(95)<1000"], // Main HTML should be very fast
    http_req_failed:        ["rate<0.01"],
  },
};

// ─── Main Test ─────────────────────────────────────────────────────────────
export default function () {
  group("Frontend Application Load", () => {
    const headers = {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 k6-load-test",
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
      "Accept-Encoding": "gzip, deflate, br",
    };

    // Test 1: Load main index.html (Root)
    const rootStart = Date.now();
    const rootRes = http.get(FRONTEND_URL, {
      headers: headers,
      tags: { name: "frontend_root" }
    });
    frontendDuration.add(Date.now() - rootStart);

    const rootOk = check(rootRes, {
      "root: status 200": (r) => r.status === 200,
      "root: is html": (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('text/html'),
    });
    frontendSuccessRate.add(rootOk);

    sleep(0.1);

    // Test 2: Simulating routing to /login
    const loginStart = Date.now();
    const loginRes = http.get(`${FRONTEND_URL}/login`, {
      headers: headers,
      tags: { name: "frontend_login" }
    });
    frontendDuration.add(Date.now() - loginStart);
    
    const loginOk = check(loginRes, {
      "login: status 200": (r) => r.status === 200,
    });
    frontendSuccessRate.add(loginOk);
  });

  sleep(Math.random() * 2 + 1); // 1-3s think time before reloading
}

// ─── Summary ───────────────────────────────────────────────────────────────
export function handleSummary(data) {
  console.log("=== FRONTEND LOAD TEST SUMMARY ===");
  console.log(`Frontend Success Rate: ${(data.metrics.frontend_success_rate?.values?.rate * 100).toFixed(1)}%`);
  console.log(`Avg Response Time:     ${data.metrics.frontend_duration_ms?.values?.avg?.toFixed(0)}ms`);
  console.log(`P95 Response Time:     ${data.metrics.frontend_duration_ms?.values?.["p(95)"]?.toFixed(0)}ms`);
  return {};
}
