"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
	Sheet,
	SheetContent,
	SheetHeader,
	SheetTitle,
	SheetTrigger,
} from "@/components/ui/sheet";
import { MenuIcon, TicketIcon, LogOutIcon } from "lucide-react";
import { logout } from "@/app/actions";
import { useState } from "react";
import type { Session } from "next-auth";

interface MobileNavProps {
	session: Session | null;
}

export function MobileNav({ session }: MobileNavProps) {
	const [open, setOpen] = useState(false);

	return (
		<div className="md:hidden">
			<Sheet open={open} onOpenChange={setOpen}>
						<SheetTrigger render={<Button variant="ghost" size="icon" className="h-9 w-9" />}>
							<MenuIcon className="h-5 w-5" />
							<span className="sr-only">Abrir menú</span>
						</SheetTrigger>
				<SheetContent side="right" className="w-72">
					<SheetHeader>
						<SheetTitle className="flex items-center gap-2">
							<div className="flex h-8 w-8 items-center justify-center rounded-lg bg-foreground text-background">
								<TicketIcon className="h-4 w-4" />
							</div>
							Passly
						</SheetTitle>
					</SheetHeader>
					<nav className="flex flex-col gap-2 mt-6">
						<Link
							href="/"
							className="px-3 py-2 text-sm font-medium rounded-lg hover:bg-muted transition-colors"
							onClick={() => setOpen(false)}
						>
							Eventos
						</Link>
						{session?.user ? (
							<>
								<div className="my-2 h-px bg-border" />
								<div className="px-3 py-2">
									<p className="text-sm font-medium">{session.user.username ?? session.user.name}</p>
									<p className="text-xs text-muted-foreground">Cuenta personal</p>
								</div>
								<form action={logout}>
									<Button
										type="submit"
										variant="ghost"
										className="w-full justify-start gap-2"
										onClick={() => setOpen(false)}
									>
										<LogOutIcon className="h-4 w-4" />
										Cerrar sesión
									</Button>
								</form>
							</>
						) : (
							<>
								<div className="my-2 h-px bg-border" />
								<Link
									href="/login"
									className="px-3 py-2 text-sm font-medium rounded-lg hover:bg-muted transition-colors"
									onClick={() => setOpen(false)}
								>
									Iniciar sesión
								</Link>
								<Link
									href="/register"
									className="px-3 py-2 text-sm font-medium rounded-lg bg-foreground text-background text-center hover:bg-foreground/90 transition-colors"
									onClick={() => setOpen(false)}
								>
									Registrarse
								</Link>
							</>
						)}
					</nav>
				</SheetContent>
			</Sheet>
		</div>
	);
}
