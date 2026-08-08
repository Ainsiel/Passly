import { auth } from "@/auth";
import { fetchMyReservations } from "@/lib/booking";
import { notFound, redirect } from "next/navigation";
import Link from "next/link";
import { TicketCard } from "@/features/reservations/ticket-card";
import { formatDate, formatPrice } from "@/lib/utils";
import { ArrowLeftIcon, CalendarIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";

interface Props {
	params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: Props) {
	const { id } = await params;
	return { title: `Reserva ${id} — Passly` };
}

export default async function ReservationTicketsPage({ params }: Props) {
	const session = await auth();
	if (!session?.accessToken) redirect("/login");

	const { id } = await params;
	const reservations = await fetchMyReservations(session.accessToken);
	const reservation = reservations.find((r) => r.id === id);

	if (!reservation) notFound();

	return (
		<div className="flex flex-col gap-6">
			<div className="flex flex-col gap-4">
				<Link
					href="/mis-reservas"
					className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors w-fit"
				>
					<ArrowLeftIcon className="h-4 w-4" />
					Mis reservas
				</Link>

				<div>
					<h1 className="text-2xl font-bold tracking-tight">{reservation.eventName}</h1>
					<div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground mt-2">
						<div className="flex items-center gap-1.5">
							<CalendarIcon className="h-4 w-4" />
							<time dateTime={reservation.startsAt}>
								{formatDate(reservation.startsAt)}
							</time>
						</div>
						<span>{formatPrice(reservation.price)}</span>
						<Badge variant="secondary" className="bg-green-500/10 text-green-600 border-green-500/20">
							Activa
						</Badge>
					</div>
				</div>
			</div>

			<section aria-labelledby="tickets-heading">
				<h2 id="tickets-heading" className="text-lg font-semibold mb-4">
					Tus tickets ({reservation.tickets.length})
				</h2>
				<div className="grid gap-4">
					{reservation.tickets.map((ticket, i) => (
						<TicketCard key={ticket.code} ticket={ticket} index={i} />
					))}
				</div>
			</section>
		</div>
	);
}
