import { redirect, notFound } from "next/navigation";
import { auth } from "@/auth";
import { fetchEvent } from "@/lib/catalog";
import { updateEvent } from "@/app/actions";
import { EventForm } from "@/features/admin/event-form";
import type { Metadata } from "next";

interface Props {
	params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
	const { id } = await params;
	const event = await fetchEvent(id);
	return {
		title: event
			? `Editar ${event.name} — Admin — Passly`
			: "Evento no encontrado — Admin — Passly",
	};
}

export default async function EditarEventoPage({ params }: Props) {
	const session = await auth();
	if (!session?.user) redirect("/login");
	if (!session.user.roles?.includes("ADMIN")) redirect("/");

	const { id } = await params;
	const event = await fetchEvent(id);

	if (!event) {
		notFound();
	}

	return (
		<div className="flex flex-col gap-6">
			<div>
				<h2 className="text-lg font-semibold">Editar evento</h2>
				<p className="text-sm text-muted-foreground">
					Modifica los datos del evento
				</p>
			</div>
			<EventForm
				initialData={event}
				action={updateEvent}
				mode="edit"
			/>
		</div>
	);
}
