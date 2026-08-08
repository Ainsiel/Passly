import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { createEvent } from "@/app/actions";
import { EventForm } from "@/features/admin/event-form";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Crear evento — Admin — Passly",
};

export default async function NuevoEventoPage() {
	const session = await auth();
	if (!session?.user) redirect("/login");
	if (!session.user.roles?.includes("ADMIN")) redirect("/");

	return (
		<div className="flex flex-col gap-6">
			<div>
				<h2 className="text-lg font-semibold">Crear nuevo evento</h2>
				<p className="text-sm text-muted-foreground">
					Completa los datos para crear un evento
				</p>
			</div>
			<EventForm action={createEvent} mode="create" />
		</div>
	);
}
