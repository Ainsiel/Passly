import Link from "next/link";
import { auth } from "@/auth";
import { login, logout, register } from "@/app/actions";
import { Button } from "@/components/ui/button";

export async function Header() {
	const session = await auth();

	return (
		<header className="sticky top-0 z-50 border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
			<div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 sm:px-6">
				<Link
					href="/"
					className="text-lg font-bold tracking-tight"
					aria-label="Passly — inicio"
				>
					Passly
				</Link>

				<nav aria-label="Principal" className="flex items-center gap-2">
					{session?.user ? (
						<form className="flex items-center gap-2">
							<span className="text-sm text-muted-foreground hidden sm:inline">
								Hola, {session.user.username ?? session.user.name}
							</span>
							<Button type="submit" formAction={logout} variant="ghost" size="sm">
								Cerrar sesión
							</Button>
						</form>
					) : (
						<div className="flex items-center gap-2">
							<form action={login}>
								<Button type="submit" variant="ghost" size="sm">
									Iniciar sesión
								</Button>
							</form>
							<form action={register}>
								<Button type="submit" size="sm">
									Registrarse
								</Button>
							</form>
						</div>
					)}
				</nav>
			</div>
		</header>
	);
}
