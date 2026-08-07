import Link from "next/link";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { register } from "@/app/actions";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Crear cuenta — Passly",
};

export default function RegisterPage() {
	return (
		<div className="flex min-h-[60vh] items-center justify-center">
			<Card className="w-full max-w-sm">
				<CardHeader className="text-center">
					<CardTitle className="text-xl">Crear cuenta</CardTitle>
					<CardDescription>
						Regístrate para empezar a reservar entradas
					</CardDescription>
				</CardHeader>
				<CardContent className="flex flex-col gap-4">
					<form action={register}>
						<Button type="submit" className="w-full" size="lg">
							Registrarse con Keycloak
						</Button>
					</form>
				</CardContent>
				<CardFooter className="justify-center">
					<p className="text-sm text-muted-foreground">
						¿Ya tienes cuenta?{" "}
						<Link href="/login" className="text-primary hover:underline">
							Inicia sesión
						</Link>
					</p>
				</CardFooter>
			</Card>
		</div>
	);
}
