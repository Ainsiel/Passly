export type EventCategory =
	| "CONCIERTO"
	| "TEATRO"
	| "EXPOSICION"
	| "DEPORTES"
	| "OTRA"
	| "FESTIVAL"
	| "CINE"
	| "CONFERENCIA"
	| "TALLER";

export type EventSummary = {
	id: string;
	name: string;
	category: EventCategory;
	venue: string;
	startsAt: string;
	price: number;
	available: number;
};

export type EventDetail = EventSummary & {
	description: string;
	capacity: number;
	reservedTickets: number;
};

export type PageMeta = {
	number: number;
	size: number;
	totalElements: number;
	totalPages: number;
};

export type PagedEvents = {
	content: EventSummary[];
	page: PageMeta;
};

export type MeResponse = {
	username: string | null;
	roles: string[];
};

export type MeResult =
	| { status: number; ok: false; body: null }
	| { status: number; ok: true; body: MeResponse };

const CATALOG_BASE_URL =
	process.env.PASSLY_CATALOG_URL ?? "http://localhost:8090/api/catalog";

const CATALOG_ME_URL =
	process.env.PASSLY_CATALOG_ME_URL ?? `${CATALOG_BASE_URL}/me`;

export async function fetchMe(accessToken?: string): Promise<MeResult> {
	const response = await fetch(CATALOG_ME_URL, {
		headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
		cache: "no-store",
	});
	if (!response.ok) {
		return { status: response.status, ok: false, body: null };
	}
	return {
		status: response.status,
		ok: true,
		body: (await response.json()) as MeResponse,
	};
}

export async function fetchEvents(params: {
	q?: string;
	category?: string;
	date?: string;
	venue?: string;
	page?: number;
	size?: number;
	sort?: string;
}): Promise<PagedEvents> {
	const searchParams = new URLSearchParams();
	if (params.q) searchParams.set("q", params.q);
	if (params.category) searchParams.set("category", params.category);
	if (params.date) searchParams.set("date", params.date);
	if (params.venue) searchParams.set("venue", params.venue);
	if (params.page !== undefined) searchParams.set("page", String(params.page));
	if (params.size !== undefined) searchParams.set("size", String(params.size));
	if (params.sort) searchParams.set("sort", params.sort);

	const url = `${CATALOG_BASE_URL}/events?${searchParams.toString()}`;
	const response = await fetch(url, { cache: "no-store" });
	if (!response.ok) {
		return {
			content: [],
			page: { number: 0, size: 20, totalElements: 0, totalPages: 0 },
		};
	}
	return (await response.json()) as PagedEvents;
}

export async function fetchEvent(id: string): Promise<EventDetail | null> {
	const response = await fetch(`${CATALOG_BASE_URL}/events/${id}`, {
		cache: "no-store",
	});
	if (!response.ok) return null;
	return (await response.json()) as EventDetail;
}
