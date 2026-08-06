"use server";

import { signIn, signOut } from "@/auth";

export async function login() {
	await signIn("keycloak", { redirectTo: "/" });
}

export async function register() {
	await signIn("keycloak-register", { redirectTo: "/" });
}

export async function logout() {
	await signOut({ redirectTo: "/" });
}
