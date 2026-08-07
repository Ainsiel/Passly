"use server";

import { signIn, signOut } from "@/auth";
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
