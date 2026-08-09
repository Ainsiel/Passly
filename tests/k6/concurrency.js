// tests/k6/concurrency.js — Concurrencia: 100 usuarios compitiendo por últimos tickets

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { BOOKING_URL, EVENT_ID, authHeaders, makeReservationBody } from './config.js';

const errorRate = new Rate('errors');
const successCount = new Counter('reservations_ok');
const conflictCount = new Counter('reservations_conflict');
const soldOutCount = new Counter('sold_out');

export const options = {
  scenarios: {
    concurrency: {
      executor: 'per-vu-iterations',
      vus: 100,
      iterations: 1,
    },
  },
  thresholds: {
    reservations_ok: ['count>=20'],
    reservations_conflict: ['count>=50'],
  },
};

export default function () {
  const userId = `conc-${__VU}`;
  const payload = makeReservationBody(userId, EVENT_ID, 1);
  const params = authHeaders(userId);

  const res = http.post(`${BOOKING_URL}/reservations`, payload, {
    headers: params,
    tags: { scenario: 'concurrency' },
  });

  const ok = check(res, {
    'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
  });

  if (res.status === 201) {
    successCount.add(1);
  } else if (res.status === 409) {
    const body = res.body;
    if (body && body.includes('sold-out')) {
      soldOutCount.add(1);
    } else {
      conflictCount.add(1);
    }
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
  lines.push('=== CONCURRENCY RESULTS ===');
  lines.push(`  Total VUs:      ${data.metrics.vus_max?.values?.value || 100}`);
  lines.push(`  Success (201):  ${data.metrics.reservations_ok?.values?.count || 0}`);
  lines.push(`  Conflicts (409): ${data.metrics.reservations_conflict?.values?.count || 0}`);
  lines.push(`  Sold-out (409): ${data.metrics.sold_out?.values?.count || 0}`);
  lines.push(`  Error rate:     ${((data.metrics.errors?.values?.rate || 0) * 100).toFixed(2)}%`);
  lines.push('');

  const passed = data.root_group?.checks?.every(c => c.fails === 0) ?? false;
  lines.push(passed ? 'RESULT: PASS' : 'RESULT: FAIL');
  lines.push('');

  return lines.join('\n');
}
