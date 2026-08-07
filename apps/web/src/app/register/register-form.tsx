"use client";

import { useActionState } from "react";
import { register, type AuthState } from "@/app/actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Loader2Icon } from "lucide-react";

export function RegisterForm() {
	const [state, formAction, isPending] = useActionState<AuthState, FormData>(
		register,
		{ error: null },
	);

	return (
		<form action={formAction} className="flex flex-col gap-4">
			{state.error && (
				<div className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">
					{state.error}
				</div>
			)}

			<div className="grid grid-cols-2 gap-3">
				<div className="flex flex-col gap-1.5">
					<label htmlFor="firstName" className="text-sm font-medium">
						Nombre
					</label>
					<Input
						id="firstName"
						name="firstName"
						type="text"
						placeholder="Juan"
						autoComplete="given-name"
						required
					/>
				</div>
				<div className="flex flex-col gap-1.5">
					<label htmlFor="lastName" className="text-sm font-medium">
						Apellido
					</label>
					<Input
						id="lastName"
						name="lastName"
						type="text"
						placeholder="Pérez"
						autoComplete="family-name"
						required
					/>
				</div>
			</div>

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
				<label htmlFor="username" className="text-sm font-medium">
					Nombre de usuario
				</label>
				<Input
					id="username"
					name="username"
					type="text"
					placeholder="juanperez"
					autoComplete="username"
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
					autoComplete="new-password"
					minLength={6}
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
					"Crear cuenta"
				)}
			</Button>
		</form>
	);
}
