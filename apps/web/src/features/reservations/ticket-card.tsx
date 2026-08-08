import { Badge } from "@/components/ui/badge";
import { QRCodeImage } from "@/components/ui/qr-code";
import type { TicketResponse } from "@/lib/booking";

interface TicketCardProps {
	ticket: TicketResponse;
	index: number;
}

export async function TicketCard({ ticket, index }: TicketCardProps) {
	return (
		<div className="rounded-xl border border-border/60 bg-card p-6 flex flex-col sm:flex-row gap-6 items-center">
			<div className="flex-1 flex flex-col gap-3">
				<div className="flex items-center gap-2">
					<Badge variant="secondary">Ticket {index + 1}</Badge>
				</div>
				<div className="flex flex-col gap-1">
					<p className="text-xs text-muted-foreground">Código</p>
					<p className="font-mono text-sm font-medium break-all">{ticket.code}</p>
				</div>
			</div>
			<div className="flex-shrink-0">
				<QRCodeImage value={ticket.qr} />
			</div>
		</div>
	);
}
