import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { formatDate, formatPrice } from "@/lib/utils";
import type { ReservationResponse } from "@/lib/booking";
import { TicketIcon, CalendarIcon } from "lucide-react";

interface ReservationListProps {
	reservations: ReservationResponse[];
}

export function ReservationList({ reservations }: ReservationListProps) {
	if (reservations.length === 0) {
		return (
			<div className="flex flex-col items-center gap-4 py-12 text-center">
				<div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted">
					<TicketIcon className="h-8 w-8 text-muted-foreground" />
				</div>
				<div>
					<p className="text-lg font-medium">No tienes reservas</p>
					<p className="text-sm text-muted-foreground">
						Explora los eventos disponibles y reserva tu primera entrada.
					</p>
				</div>
				<Link
					href="/"
					className="inline-flex items-center justify-center rounded-lg bg-foreground px-4 py-2 text-sm font-medium text-background hover:bg-foreground/90 transition-colors"
				>
					Explorar eventos
				</Link>
			</div>
		);
	}

	return (
		<div className="flex flex-col gap-4">
			{reservations.map((reservation) => (
				<Link
					key={reservation.id}
					href={`/reservas/${reservation.id}/tickets`}
					className="group rounded-xl border border-border/60 bg-card p-6 transition-colors hover:border-foreground/20"
				>
					<div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
						<div className="flex flex-col gap-1">
							<h3 className="font-semibold group-hover:text-foreground transition-colors">
								{reservation.eventName}
							</h3>
							<div className="flex items-center gap-4 text-sm text-muted-foreground">
								<div className="flex items-center gap-1.5">
									<CalendarIcon className="h-3.5 w-3.5" />
									<time dateTime={reservation.startsAt}>
										{formatDate(reservation.startsAt)}
									</time>
								</div>
								<span>{formatPrice(reservation.price)}</span>
							</div>
						</div>
						<div className="flex items-center gap-3">
							<Badge variant="secondary" className="bg-green-500/10 text-green-600 border-green-500/20">
								{reservation.tickets.length} ticket{reservation.tickets.length > 1 ? "s" : ""}
							</Badge>
							<TicketIcon className="h-4 w-4 text-muted-foreground group-hover:text-foreground transition-colors" />
						</div>
					</div>
				</Link>
			))}
		</div>
	);
}
