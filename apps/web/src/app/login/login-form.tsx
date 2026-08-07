"use client";

import { useActionState } from "react";
import { login, type AuthState } from "@/app/actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Loader2Icon } from "lucide-react";

export function LoginForm() {
	const [state, formAction, isPending] = useActionState<AuthState, FormData>(
		login,
		{ error: null },
	);

	return (
		<form action={formAction} className="flex flex-col gap-4">
			{state.error && (
				<div className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">
					{state.error}
				</div>
			)}

			<div className="flex flex-col gap-1.5">
				<label htmlFor="email" className="text-sm font-medium">
					Email
				</label>
				<Input
					id="email"
					name="email"
					type="email"
					placeholder="tu@email.com"
					autoComplete="email"
					required
				/>
			</div>

			<div className="flex flex-col gap-1.5">
				<label htmlFor="password" className="text-sm font-medium">
					Contraseña
				</label>
				<Input
					id="password"
					name="password"
					type="password"
					placeholder="••••••••"
					autoComplete="current-password"
					required
				/>
			</div>

			<Button
				type="submit"
				className="w-full h-11 bg-foreground text-background hover:bg-foreground/90 font-medium"
				size="lg"
				disabled={isPending}
			>
				{isPending ? (
					<Loader2Icon className="h-4 w-4 animate-spin" />
				) : (
					"Iniciar sesión"
				)}
			</Button>
		</form>
	);
}
