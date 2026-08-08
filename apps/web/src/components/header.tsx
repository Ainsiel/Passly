import Link from "next/link";
import { auth } from "@/auth";
import { logout } from "@/app/actions";
import { Button } from "@/components/ui/button";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuSeparator,
	DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { TicketIcon, LogOutIcon, UserIcon, CalendarCheckIcon } from "lucide-react";
import { ThemeToggle } from "@/components/theme-toggle";
import { MobileNav } from "@/components/mobile-nav";

export async function Header() {
	const session = await auth();
	const username = session?.user?.username ?? session?.user?.name ?? "";
	const initials = username
		.split(" ")
		.map((n) => n[0])
		.join("")
		.toUpperCase()
		.slice(0, 2);

	return (
		<header className="sticky top-0 z-50 border-b border-border/40 bg-background/80 backdrop-blur-xl supports-[backdrop-filter]:bg-background/60">
			<div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6">
				<div className="flex items-center gap-8">
					<Link
						href="/"
						className="flex items-center gap-2 text-lg font-bold tracking-tight transition-opacity hover:opacity-80"
						aria-label="Passly — inicio"
					>
						<div className="flex h-8 w-8 items-center justify-center rounded-lg bg-foreground text-background">
							<TicketIcon className="h-4 w-4" />
						</div>
						<span>Passly</span>
					</Link>

					<nav aria-label="Principal" className="hidden md:flex items-center gap-1">
						<Link
							href="/"
							className="px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
						>
							Eventos
						</Link>
						{session?.user && (
							<Link
								href="/mis-reservas"
								className="px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
							>
								Mis reservas
							</Link>
						)}
					</nav>
				</div>

				<div className="flex items-center gap-2">
					<ThemeToggle />

					{session?.user ? (
						<DropdownMenu>
							<DropdownMenuTrigger
								render={
									<Button variant="ghost" className="relative h-9 w-9 rounded-full">
										<Avatar className="h-9 w-9">
											<AvatarFallback className="bg-muted text-xs font-medium">
												{initials || <UserIcon className="h-4 w-4" />}
											</AvatarFallback>
										</Avatar>
										<span className="sr-only">Menú de usuario</span>
									</Button>
								}
							/>
							<DropdownMenuContent className="w-56" align="end">
								<div className="flex items-center gap-2 p-2">
									<Avatar className="h-8 w-8">
										<AvatarFallback className="bg-muted text-xs font-medium">
											{initials || <UserIcon className="h-4 w-4" />}
										</AvatarFallback>
									</Avatar>
									<div className="flex flex-col space-y-0.5">
										<p className="text-sm font-medium">{username}</p>
										<p className="text-xs text-muted-foreground">Cuenta personal</p>
									</div>
								</div>
								<DropdownMenuSeparator />
								<DropdownMenuItem>
									<Link href="/mis-reservas" className="flex items-center gap-2 w-full">
										<CalendarCheckIcon className="h-4 w-4" />
										Mis reservas
									</Link>
								</DropdownMenuItem>
								<DropdownMenuSeparator />
								<DropdownMenuItem>
									<form action={logout} className="w-full">
										<Button type="submit" variant="ghost" className="w-full justify-start gap-2 h-8 px-2">
											<LogOutIcon className="h-4 w-4" />
											Cerrar sesión
										</Button>
									</form>
								</DropdownMenuItem>
							</DropdownMenuContent>
						</DropdownMenu>
					) : (
						<div className="hidden sm:flex items-center gap-2">
							<Button variant="ghost" size="sm" render={<Link href="/login" />}>
								Iniciar sesión
							</Button>
							<Button size="sm" className="bg-foreground text-background hover:bg-foreground/90" render={<Link href="/register" />}>
								Registrarse
							</Button>
						</div>
					)}

					<MobileNav session={session} />
				</div>
			</div>
		</header>
	);
}
