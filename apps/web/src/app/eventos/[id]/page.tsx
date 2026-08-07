import { notFound } from "next/navigation";
import { fetchEvent } from "@/lib/catalog";
import { EventDetail } from "@/features/events/event-detail";
import type { Metadata } from "next";

interface Props {
	params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
	const { id } = await params;
	const event = await fetchEvent(id);
	if (!event) return { title: "Evento no encontrado — Passly" };
	return { title: `${event.name} — Passly` };
}

export default async function EventDetailPage({ params }: Props) {
	const { id } = await params;
	const event = await fetchEvent(id);

	if (!event) {
		notFound();
	}

	return <EventDetail event={event} />;
}
