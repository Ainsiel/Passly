"use server";

import { signIn, signOut } from "@/auth";
import { auth } from "@/auth";
import { createReservation } from "@/lib/booking";
import { AuthError } from "next-auth";

export type AuthState = {
	error: string | null;
};

export async function login(
	_prev: AuthState,
	formData: FormData,
): Promise<AuthState> {
	const email = formData.get("email") as string | null;
	const password = formData.get("password") as string | null;

	if (!email || !password) {
		return { error: "Email y contraseña son obligatorios" };
	}

	try {
		await signIn("keycloak-direct", {
			email,
			password,
			redirectTo: "/",
		});
	} catch (error) {
		if (error instanceof AuthError) {
			return { error: "Credenciales incorrectas" };
		}
		throw error;
	}

	return { error: null };
}

export async function register(
	_prev: AuthState,
	formData: FormData,
): Promise<AuthState> {
	const firstName = formData.get("firstName") as string | null;
	const lastName = formData.get("lastName") as string | null;
	const email = formData.get("email") as string | null;
	const username = formData.get("username") as string | null;
	const password = formData.get("password") as string | null;

	if (!firstName || !lastName || !email || !username || !password) {
		return { error: "Todos los campos son obligatorios" };
	}

	if (password.length < 6) {
		return { error: "La contraseña debe tener al menos 6 caracteres" };
	}

	const baseUrl = process.env.KEYCLOAK_BASE_URL;
	const realm = process.env.KEYCLOAK_REALM;
	const adminClientId = process.env.KEYCLOAK_ADMIN_CLIENT_ID;
	const adminUsername = process.env.KEYCLOAK_ADMIN_USERNAME;
	const adminPassword = process.env.KEYCLOAK_ADMIN_PASSWORD;

	if (!baseUrl || !realm || !adminClientId || !adminUsername || !adminPassword) {
		return { error: "Configuración de administrador no disponible" };
	}

	try {
		const tokenResponse = await fetch(
			`${baseUrl}/realms/master/protocol/openid-connect/token`,
			{
				method: "POST",
				headers: { "Content-Type": "application/x-www-form-urlencoded" },
				body: new URLSearchParams({
					grant_type: "password",
					client_id: adminClientId,
					username: adminUsername,
					password: adminPassword,
				}),
			},
		);

		if (!tokenResponse.ok) {
			return { error: "Error al autenticar con el administrador" };
		}

		const tokenData = (await tokenResponse.json()) as { access_token: string };
		const adminToken = tokenData.access_token;

		const createResponse = await fetch(
			`${baseUrl}/admin/realms/${realm}/users`,
			{
				method: "POST",
				headers: {
					"Content-Type": "application/json",
					Authorization: `Bearer ${adminToken}`,
				},
				body: JSON.stringify({
					username,
					email,
					firstName,
					lastName,
					enabled: true,
					emailVerified: false,
					credentials: [
						{
							type: "password",
							value: password,
							temporary: false,
						},
					],
					realmRoles: ["USER"],
				}),
			},
		);

		if (!createResponse.ok) {
			if (createResponse.status === 409) {
				return { error: "El nombre de usuario o email ya está en uso" };
			}
			return { error: "Error al crear la cuenta" };
		}

		await signIn("keycloak-direct", {
			email,
			password,
			redirectTo: "/",
		});
	} catch (error) {
		if (error instanceof AuthError) {
			return { error: "Credenciales incorrectas" };
		}
		throw error;
	}

	return { error: null };
}

export async function logout() {
	await signOut({ redirectTo: "/" });
}

export type ReserveState = {
	ok: boolean;
	error?: string;
	reservationId?: string;
};

export async function reserveTickets(
	_prev: ReserveState,
	formData: FormData,
): Promise<ReserveState> {
	try {
		const session = await auth();
		if (!session?.accessToken) {
			return { ok: false, error: "Debes iniciar sesión para reservar" };
		}

		const eventId = Number(formData.get("eventId"));
		const quantity = Number(formData.get("quantity"));
		const idempotencyKey = formData.get("idempotencyKey") as string;

		let email = "";
		try {
			const payload = session.accessToken.split(".")[1];
			const decoded = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
			email = decoded.email ?? "";
		} catch {
			// fallback: email will be empty
		}

		if (!eventId || !quantity || quantity < 1 || quantity > 4) {
			return { ok: false, error: "Cantidad inválida (1-4 tickets)" };
		}

		if (!idempotencyKey) {
			return { ok: false, error: "Falta clave de idempotencia" };
		}

		const result = await createReservation(
			session.accessToken,
			eventId,
			quantity,
			idempotencyKey,
			email,
		);

		if (!result.ok) {
			const messages: Record<number, string> = {
				409: "Ya tienes una reserva para este evento o está agotado",
				404: "Evento no encontrado",
				400: "Solicitud inválida",
				401: "Debes iniciar sesión",
			};
			return {
				ok: false,
				error: messages[result.status] ?? `Error del servidor (${result.status})`,
			};
		}

		return { ok: true, reservationId: result.data.id };
	} catch (err) {
		return { ok: false, error: `Error inesperado al reservar: ${String(err)}` };
	}
}

const CATALOG_BASE_URL =
	process.env.PASSLY_CATALOG_URL ?? "http://localhost:8090/api/catalog";

export type AdminState = {
	ok: boolean;
	error?: string;
	eventId?: string;
};

export async function createEvent(
	_prev: AdminState,
	formData: FormData,
): Promise<AdminState> {
	const session = await auth();
	if (!session?.accessToken) {
		return { ok: false, error: "Debes iniciar sesión" };
	}
	if (!session.user?.roles?.includes("ADMIN")) {
		return { ok: false, error: "No tienes permisos de administrador" };
	}

	const name = formData.get("name") as string | null;
	const description = formData.get("description") as string | null;
	const category = formData.get("category") as string | null;
	const venue = formData.get("venue") as string | null;
	const startsAt = formData.get("startsAt") as string | null;
	const price = formData.get("price") as string | null;
	const capacity = formData.get("capacity") as string | null;

	if (!name || !description || !category || !venue || !startsAt || price === null || capacity === null) {
		return { ok: false, error: "Todos los campos son obligatorios" };
	}

	const priceNum = Number(price);
	const capacityNum = Number(capacity);

	if (isNaN(priceNum) || priceNum < 0) {
		return { ok: false, error: "El precio debe ser un número positivo" };
	}
	if (isNaN(capacityNum) || capacityNum < 0) {
		return { ok: false, error: "La capacidad debe ser un número positivo" };
	}

	const response = await fetch(`${CATALOG_BASE_URL}/events`, {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
			Authorization: `Bearer ${session.accessToken}`,
		},
		body: JSON.stringify({
			name,
			description,
			category,
			venue,
			startsAt,
			price: priceNum,
			capacity: capacityNum,
		}),
	});

	if (!response.ok) {
		if (response.status === 403) {
			return { ok: false, error: "No tienes permisos para crear eventos" };
		}
		return { ok: false, error: `Error del servidor (${response.status})` };
	}

	const result = (await response.json()) as { id: string };
	return { ok: true, eventId: result.id };
}

export async function updateEvent(
	_prev: AdminState,
	formData: FormData,
): Promise<AdminState> {
	const session = await auth();
	if (!session?.accessToken) {
		return { ok: false, error: "Debes iniciar sesión" };
	}
	if (!session.user?.roles?.includes("ADMIN")) {
		return { ok: false, error: "No tienes permisos de administrador" };
	}

	const id = formData.get("id") as string | null;
	const name = formData.get("name") as string | null;
	const description = formData.get("description") as string | null;
	const category = formData.get("category") as string | null;
	const venue = formData.get("venue") as string | null;
	const startsAt = formData.get("startsAt") as string | null;
	const price = formData.get("price") as string | null;
	const capacity = formData.get("capacity") as string | null;

	if (!id) {
		return { ok: false, error: "ID del evento requerido" };
	}
	if (!name || !description || !category || !venue || !startsAt || price === null || capacity === null) {
		return { ok: false, error: "Todos los campos son obligatorios" };
	}

	const priceNum = Number(price);
	const capacityNum = Number(capacity);

	if (isNaN(priceNum) || priceNum < 0) {
		return { ok: false, error: "El precio debe ser un número positivo" };
	}
	if (isNaN(capacityNum) || capacityNum < 0) {
		return { ok: false, error: "La capacidad debe ser un número positivo" };
	}

	const response = await fetch(`${CATALOG_BASE_URL}/events/${id}`, {
		method: "PUT",
		headers: {
			"Content-Type": "application/json",
			Authorization: `Bearer ${session.accessToken}`,
		},
		body: JSON.stringify({
			name,
			description,
			category,
			venue,
			startsAt,
			price: priceNum,
			capacity: capacityNum,
		}),
	});

	if (!response.ok) {
		if (response.status === 403) {
			return { ok: false, error: "No tienes permisos para editar eventos" };
		}
		if (response.status === 404) {
			return { ok: false, error: "Evento no encontrado" };
		}
		return { ok: false, error: `Error del servidor (${response.status})` };
	}

	return { ok: true };
}

export async function deleteEvent(
	_prev: AdminState,
	formData: FormData,
): Promise<AdminState> {
	const session = await auth();
	if (!session?.accessToken) {
		return { ok: false, error: "Debes iniciar sesión" };
	}
	if (!session.user?.roles?.includes("ADMIN")) {
		return { ok: false, error: "No tienes permisos de administrador" };
	}

	const id = formData.get("id") as string | null;

	if (!id) {
		return { ok: false, error: "ID del evento requerido" };
	}

	const response = await fetch(`${CATALOG_BASE_URL}/events/${id}`, {
		method: "DELETE",
		headers: {
			Authorization: `Bearer ${session.accessToken}`,
		},
	});

	if (!response.ok) {
		if (response.status === 403) {
			return { ok: false, error: "No tienes permisos para eliminar eventos" };
		}
		if (response.status === 404) {
			return { ok: false, error: "Evento no encontrado" };
		}
		return { ok: false, error: `Error del servidor (${response.status})` };
	}

	return { ok: true };
}
