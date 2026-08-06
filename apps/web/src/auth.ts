import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";
import type { JWT } from "next-auth/jwt";

const keycloakAuthUrl = process.env.AUTH_KEYCLOAK_AUTH_URL;
const keycloakTokenUrl = process.env.AUTH_KEYCLOAK_TOKEN_URL;
const keycloakUserinfoUrl = process.env.AUTH_KEYCLOAK_USERINFO_URL;

const keycloakProvider = {
	clientId: process.env.AUTH_KEYCLOAK_ID ?? "",
	clientSecret: process.env.AUTH_KEYCLOAK_SECRET ?? "",
	issuer: process.env.AUTH_KEYCLOAK_ISSUER ?? "",
	...(keycloakAuthUrl
		? {
				authorization: {
					url: keycloakAuthUrl,
				},
			}
		: {}),
	...(keycloakTokenUrl
		? {
				token: {
					url: keycloakTokenUrl,
				},
			}
		: {}),
	...(keycloakUserinfoUrl
		? {
				userinfo: {
					url: keycloakUserinfoUrl,
				},
			}
		: {}),
};

const REFRESH_BEFORE_EXPIRY_MS = 60_000;

async function refreshAccessToken(token: JWT): Promise<JWT | null> {
	if (!token.refreshToken) return token;
	const tokenUrl = process.env.AUTH_KEYCLOAK_TOKEN_URL;
	if (!tokenUrl) return token;
	try {
		const response = await fetch(tokenUrl, {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: new URLSearchParams({
				grant_type: "refresh_token",
				refresh_token: token.refreshToken as string,
				client_id: process.env.AUTH_KEYCLOAK_ID ?? "",
			}),
		});
		if (!response.ok) return null;
		const tokens = (await response.json()) as {
			access_token: string;
			refresh_token?: string;
			expires_in: number;
		};
		return {
			...token,
			accessToken: tokens.access_token,
			...(tokens.refresh_token
				? { refreshToken: tokens.refresh_token }
				: {}),
			accessTokenExpires: Date.now() + tokens.expires_in * 1000,
		};
	} catch {
		return token;
	}
}

export const { handlers, auth, signIn, signOut } = NextAuth({
	providers: [
		Keycloak(keycloakProvider),
		Keycloak({
			...keycloakProvider,
			id: "keycloak-register",
			name: "Keycloak (Registro)",
			authorization: {
				url: keycloakAuthUrl,
				params: { prompt: "create" },
			},
		}),
	],
	session: { strategy: "jwt" },
	callbacks: {
		async jwt({ token, account, profile }) {
			if (account) {
				token.accessToken = account.access_token;
				token.refreshToken = account.refresh_token;
				token.accessTokenExpires = account.expires_at
					? account.expires_at * 1000
					: undefined;
			}
			if (profile) {
				token.username = profile.preferred_username as string | undefined;
				const realmAccess = profile.realm_access as
					| { roles?: string[] }
					| undefined;
				token.roles = Array.isArray(realmAccess?.roles)
					? realmAccess.roles
					: [];
			}
			if (
				token.accessTokenExpires &&
				Date.now() >=
					(token.accessTokenExpires as number) - REFRESH_BEFORE_EXPIRY_MS
			) {
				return refreshAccessToken(token);
			}
			return token;
		},
		async session({ session, token }) {
			session.user.name = token.username ?? null;
			session.user.roles = token.roles ?? [];
			session.accessToken = token.accessToken;
			return session;
		},
	},
});
