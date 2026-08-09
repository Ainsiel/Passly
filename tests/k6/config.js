// tests/k6/config.js — Configuración compartida para todos los escenarios k6

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';
export const BOOKING_URL = __ENV.BOOKING_URL || 'http://localhost:8082';
export const EVENT_ID = parseInt(__ENV.EVENT_ID || '9999');
export const MAX_TICKETS = parseInt(__ENV.MAX_TICKETS || '4');

export const HEADERS = {
  'Content-Type': 'application/json',
};

export function authHeaders(userId) {
  return {
    ...HEADERS,
    'Authorization': `Bearer loadtest-user-${userId}`,
    'X-Idempotency-Key': `k6-${userId}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  };
}

export function makeReservationBody(userId, eventId, quantity) {
  return JSON.stringify({
    eventId: eventId,
    quantity: quantity,
    email: `loadtest-user-${userId}@passly.local`,
  });
}
