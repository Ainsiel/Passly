import Link from "next/link";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { login } from "@/app/actions";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Iniciar sesión — Passly",
};

export default function LoginPage() {
	return (
		<div className="flex min-h-[60vh] items-center justify-center">
			<Card className="w-full max-w-sm">
				<CardHeader className="text-center">
					<CardTitle className="text-xl">Iniciar sesión</CardTitle>
					<CardDescription>
						Accede a tu cuenta para reservar entradas
					</CardDescription>
				</CardHeader>
				<CardContent className="flex flex-col gap-4">
					<form action={login}>
						<Button type="submit" className="w-full" size="lg">
							Iniciar sesión con Keycloak
						</Button>
					</form>
				</CardContent>
				<CardFooter className="justify-center">
					<p className="text-sm text-muted-foreground">
						¿No tienes cuenta?{" "}
						<Link href="/register" className="text-primary hover:underline">
							Regístrate
						</Link>
					</p>
				</CardFooter>
			</Card>
		</div>
	);
}
