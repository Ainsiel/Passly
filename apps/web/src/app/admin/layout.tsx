import Link from "next/link";
import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { Button } from "@/components/ui/button";
import { CalendarIcon, PlusIcon, ArrowLeftIcon } from "lucide-react";

export default async function AdminLayout({
	children,
}: {
	children: React.ReactNode;
}) {
	const session = await auth();

	if (!session?.user) {
		redirect("/login");
	}

	if (!session.user.roles?.includes("ADMIN")) {
		redirect("/");
	}

	return (
		<div className="flex flex-col gap-6">
			<div className="flex items-center justify-between">
				<div className="flex items-center gap-4">
					<h1 className="text-2xl font-bold tracking-tight">Admin</h1>
					<nav className="flex items-center gap-1" aria-label="Admin">
						<Button
							variant="ghost"
							size="sm"
							render={<Link href="/admin" />}
							className="gap-2"
						>
							<CalendarIcon className="h-4 w-4" />
							Eventos
						</Button>
						<Button
							variant="ghost"
							size="sm"
							render={<Link href="/admin/nuevo" />}
							className="gap-2"
						>
							<PlusIcon className="h-4 w-4" />
							Crear evento
						</Button>
					</nav>
				</div>
				<Button
					variant="ghost"
					size="sm"
					render={<Link href="/" />}
					className="gap-2"
				>
					<ArrowLeftIcon className="h-4 w-4" />
					Volver al sitio
				</Button>
			</div>
			<div>{children}</div>
		</div>
	);
}
