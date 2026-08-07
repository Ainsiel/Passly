import Link from "next/link";
import { TicketIcon } from "lucide-react";
import type { Metadata } from "next";
import { LoginForm } from "./login-form";

export const metadata: Metadata = {
	title: "Iniciar sesión — Passly",
};

export default function LoginPage() {
	return (
		<div className="flex min-h-[70vh] items-center justify-center">
			<div className="w-full max-w-md">
				<div className="flex justify-center mb-8">
					<Link href="/" className="flex items-center gap-2">
						<div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-foreground text-background">
							<TicketIcon className="h-6 w-6" />
						</div>
					</Link>
				</div>

				<div className="rounded-2xl border border-border/60 bg-card p-8 shadow-sm">
					<div className="text-center mb-8">
						<h1 className="text-2xl font-bold tracking-tight mb-2">
							Bienvenido de vuelta
						</h1>
						<p className="text-muted-foreground">
							Inicia sesión para reservar tus entradas favoritas
						</p>
					</div>

					<LoginForm />

					<p className="text-center text-sm text-muted-foreground mt-6">
						¿No tienes cuenta?{" "}
						<Link
							href="/register"
							className="font-medium text-foreground hover:underline underline-offset-4"
						>
							Crear cuenta
						</Link>
					</p>
				</div>

				<p className="text-center text-xs text-muted-foreground mt-6">
					Al continuar, aceptas nuestros{" "}
					<Link href="#" className="hover:underline underline-offset-4">
						Términos de servicio
					</Link>{" "}
					y{" "}
					<Link href="#" className="hover:underline underline-offset-4">
						Política de privacidad
					</Link>
				</p>
			</div>
		</div>
	);
}
