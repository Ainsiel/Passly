import { DefaultSession } from "next-auth";

declare module "next-auth" {
	interface Session {
		accessToken?: string;
		user: {
			username?: string | null;
			roles?: string[];
		} & DefaultSession["user"];
	}
}

declare module "@auth/core/jwt" {
	interface JWT {
		accessToken?: string;
		username?: string;
		roles?: string[];
	}
}
