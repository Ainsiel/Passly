export type MeResponse = {
	username: string | null;
	roles: string[];
};

export type MeResult =
	| { status: number; ok: false; body: null }
	| { status: number; ok: true; body: MeResponse };

const CATALOG_ME_URL =
	process.env.PASSLY_CATALOG_ME_URL ?? "http://localhost:8090/api/catalog/me";

export async function fetchMe(accessToken?: string): Promise<MeResult> {
	const response = await fetch(CATALOG_ME_URL, {
		headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
		cache: "no-store",
	});
	if (!response.ok) {
		return { status: response.status, ok: false, body: null };
	}
	return { status: response.status, ok: true, body: (await response.json()) as MeResponse };
}
