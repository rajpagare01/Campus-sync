/**
 * K6 Load Test #1 — Authentication APIs
 * ======================================
 * Tests: POST /api/v1/auth/login, POST /api/v1/auth/refresh
 *
 * WHY TEST FIRST:
 * - Every user session starts with login → highest concurrency risk
 * - JWT generation is CPU-intensive (BCrypt + signing)
 * - If auth is slow, ALL other endpoints become slow
 *
 * Run: k6 run 01_auth_test.js
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";

// ─── Custom Metrics ────────────────────────────────────────────────────────
const loginSuccessRate = new Rate("login_success_rate");
const refreshSuccessRate = new Rate("refresh_success_rate");
const loginDuration = new Trend("login_duration_ms", true);
const refreshDuration = new Trend("refresh_duration_ms", true);
const authErrors = new Counter("auth_errors");

// ─── Config ────────────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "https://campus-sync-fpqp.onrender.com";

// Credentials via env vars — never hardcode passwords in test files!
// Run: k6 run -e EMAIL=you@example.com -e PASSWORD=yourpass 01_auth_test.js
const TEST_USERS = [
  {
    email: __ENV.EMAIL || "rajpagare011@gmail.com",
    password: __ENV.PASSWORD,
  },
];

// ─── Load Stages ───────────────────────────────────────────────────────────
export const options = {
  stages: [
    { duration: "30s", target: 10 },   // Ramp up to 10 users in 30s
    { duration: "1m",  target: 50 },   // Ramp up to 50 users in 1 min
    { duration: "2m",  target: 50 },   // Sustain 50 users for 2 min
    { duration: "30s", target: 100 },  // Spike to 100 users
    { duration: "30s", target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration:      ["p(95)<2000"],  // 95% requests under 2s
    login_success_rate:     ["rate>0.95"],   // 95%+ logins must succeed
    refresh_success_rate:   ["rate>0.95"],   // 95%+ refreshes must succeed
    login_duration_ms:      ["p(95)<1500"],  // Login under 1.5s (BCrypt is slow)
    http_req_failed:        ["rate<0.05"],   // Less than 5% failures
  },
};

// ─── Helpers ────────────────────────────────────────────────────────────────
function randomUser() {
  return TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
}

// ─── Main Test ─────────────────────────────────────────────────────────────
export default function () {
  const user = randomUser();
  const headers = { "Content-Type": "application/json" };

  // ── Step 1: Login ──────────────────────────────────────────────────────
  const loginStart = Date.now();
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: user.email, password: user.password }),
    { headers, tags: { name: "auth_login" } }
  );
  loginDuration.add(Date.now() - loginStart);

  const loginOk = check(loginRes, {
    "login: status 200":            (r) => r.status === 200,
    "login: has accessToken":       (r) => !!r.json("accessToken"),
    "login: has refreshToken":      (r) => !!r.json("refreshToken"),
    "login: response time < 2000ms": (r) => r.timings.duration < 2000,
  });

  loginSuccessRate.add(loginOk);
  if (!loginOk) {
    authErrors.add(1);
    console.error(`Login failed [${loginRes.status}]: ${loginRes.body}`);
    sleep(1);
    return;
  }

  const { accessToken, refreshToken } = loginRes.json();

  sleep(1);

  // ── Step 2: Token Refresh ──────────────────────────────────────────────
  const refreshStart = Date.now();
  const refreshRes = http.post(
    `${BASE_URL}/api/v1/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers, tags: { name: "auth_refresh" } }
  );
  refreshDuration.add(Date.now() - refreshStart);

  const refreshOk = check(refreshRes, {
    "refresh: status 200":            (r) => r.status === 200,
    "refresh: has new accessToken":   (r) => !!r.json("accessToken"),
    "refresh: response time < 1000ms": (r) => r.timings.duration < 1000,
  });

  refreshSuccessRate.add(refreshOk);
  if (!refreshOk) {
    authErrors.add(1);
    console.error(`Refresh failed [${refreshRes.status}]: ${refreshRes.body}`);
  }

  sleep(Math.random() * 2 + 1); // 1–3s think time
}

// ─── Summary ───────────────────────────────────────────────────────────────
export function handleSummary(data) {
  console.log("=== AUTH LOAD TEST SUMMARY ===");
  console.log(`Login Success Rate: ${(data.metrics.login_success_rate?.values?.rate * 100).toFixed(1)}%`);
  console.log(`Avg Login Duration: ${data.metrics.login_duration_ms?.values?.avg?.toFixed(0)}ms`);
  console.log(`P95 Login Duration: ${data.metrics.login_duration_ms?.values?.["p(95)"]?.toFixed(0)}ms`);
  return {};
}
