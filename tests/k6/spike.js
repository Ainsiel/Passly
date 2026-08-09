// tests/k6/spike.js — Spike: tráfico 10x durante 30s sobre POST /reservations

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BOOKING_URL, EVENT_ID, authHeaders, makeReservationBody } from './config.js';

const errorRate = new Rate('errors');
const reservationDuration = new Trend('reservation_duration', true);
const successCount = new Counter('reservations_ok');
const conflictCount = new Counter('reservations_conflict');

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-arrival-rate',
      startRate: 10,
      timeUnit: '1s',
      preAllocatedVUs: 300,
      maxVUs: 1000,
      stages: [
        { duration: '20s', target: 10 },
        { duration: '5s', target: 100 },
        { duration: '30s', target: 100 },
        { duration: '5s', target: 10 },
        { duration: '20s', target: 10 },
      ],
    },
  },
  thresholds: {
    errors: ['rate<0.15'],
  },
};

let userCounter = 0;

export default function () {
  const userId = `spike-${++userCounter}`;
  const payload = makeReservationBody(userId, EVENT_ID, 1);
  const params = authHeaders(userId);

  const res = http.post(`${BOOKING_URL}/reservations`, payload, {
    headers: params,
    tags: { scenario: 'spike' },
  });

  reservationDuration.add(res.timings.duration);

  const ok = check(res, {
    'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
  });

  if (res.status === 201) {
    successCount.add(1);
  } else if (res.status === 409) {
    conflictCount.add(1);
  }

  errorRate.add(!ok);

  sleep(0.01);
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  const lines = [];
  lines.push('');
  lines.push('=== SPIKE RESULTS ===');
  lines.push(`  Total requests: ${data.metrics.http_reqs?.values?.count || 0}`);
  lines.push(`  Peak RPS:       ${(data.metrics.http_reqs?.values?.rate || 0).toFixed(1)}`);
  lines.push(`  p95 latency:    ${(data.metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(1)}ms`);
  lines.push(`  p99 latency:    ${(data.metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(1)}ms`);
  lines.push(`  Success (201):  ${data.metrics.reservations_ok?.values?.count || 0}`);
  lines.push(`  Conflicts (409): ${data.metrics.reservations_conflict?.values?.count || 0}`);
  lines.push(`  Error rate:     ${((data.metrics.errors?.values?.rate || 0) * 100).toFixed(2)}%`);
  lines.push('');

  const passed = data.root_group?.checks?.every(c => c.fails === 0) ?? false;
  lines.push(passed ? 'RESULT: PASS' : 'RESULT: FAIL');
  lines.push('');

  return lines.join('\n');
}
