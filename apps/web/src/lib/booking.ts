export type TicketResponse = {
	code: string;
	qr: string;
};

export type ReservationResponse = {
	id: string;
	eventId: number;
	eventName: string;
	startsAt: string;
	price: number;
	status: "ACTIVE";
	email: string;
	createdAt: string;
	tickets: TicketResponse[];
};

const BOOKING_BASE_URL =
	process.env.PASSLY_BOOKING_URL ?? "http://localhost:8090/api/booking";

type BookingSuccess = { ok: true; data: ReservationResponse };
type BookingError = { ok: false; status: number; detail: string };
type BookingResult = BookingSuccess | BookingError;

export async function createReservation(
	accessToken: string,
	eventId: number,
	quantity: number,
	idempotencyKey: string,
	email: string,
): Promise<BookingResult> {
	let response: Response;
	try {
		response = await fetch(`${BOOKING_BASE_URL}/reservations`, {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				Authorization: `Bearer ${accessToken}`,
				"X-Idempotency-Key": idempotencyKey,
			},
			body: JSON.stringify({ eventId, quantity, email }),
			cache: "no-store",
		});
	} catch (err) {
		return { ok: false, status: 0, detail: `No se pudo conectar con el servicio de reservas: ${String(err)}` };
	}

	if (!response.ok) {
		let detail = `Error ${response.status}`;
		try {
			const body = await response.json();
			detail = body.detail ?? body.title ?? detail;
		} catch {
			// ignore parse error
		}
		return { ok: false, status: response.status, detail };
	}

	let data: ReservationResponse;
	try {
		data = (await response.json()) as ReservationResponse;
	} catch {
		return { ok: false, status: response.status, detail: "Respuesta inválida del servidor" };
	}
	return { ok: true, data };
}

export async function fetchMyReservations(
	accessToken: string,
): Promise<ReservationResponse[]> {
	const response = await fetch(`${BOOKING_BASE_URL}/reservations`, {
		headers: { Authorization: `Bearer ${accessToken}` },
		cache: "no-store",
	});
	if (!response.ok) return [];
	return (await response.json()) as ReservationResponse[];
}
