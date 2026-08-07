import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { CalendarIcon, MapPinIcon } from "lucide-react";
import { cn, formatDate, formatPrice, categoryLabel, categoryGradientClass } from "@/lib/utils";
import type { EventSummary } from "@/lib/catalog";

interface EventCardProps {
	event: EventSummary;
}

export function EventCard({ event }: EventCardProps) {
	const isSoldOut = event.available === 0;

	return (
		<article
			className={cn(
				"group relative flex flex-col overflow-hidden rounded-xl border border-border/60 bg-card text-card-foreground transition-all duration-200 hover:shadow-lg hover:shadow-black/5 hover:border-border hover:-translate-y-0.5",
				isSoldOut && "opacity-60"
			)}
		>
			<Link
				href={`/eventos/${event.id}`}
				className="absolute inset-0 z-10"
				aria-label={`Ver detalle de ${event.name}, ${categoryLabel(event.category)}`}
			>
				<span className="sr-only">Ver detalle</span>
			</Link>

			{/* Category Gradient Header */}
			<div className={cn(
				"relative h-32 sm:h-36 bg-gradient-to-br opacity-90",
				categoryGradientClass(event.category)
			)}>
				<div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_30%,rgba(255,255,255,0.2),transparent_60%)]" />
				<div className="absolute top-3 left-3">
					<Badge
						variant="secondary"
						className="bg-white/90 text-black border-0 font-medium backdrop-blur-sm pointer-events-none"
					>
						{categoryLabel(event.category)}
					</Badge>
				</div>
				{isSoldOut && (
					<div className="absolute top-3 right-3">
						<Badge variant="destructive" className="font-medium pointer-events-none">
							Agotado
						</Badge>
					</div>
				)}
			</div>

			{/* Content */}
			<div className="flex flex-1 flex-col p-4 sm:p-5 pointer-events-none">
				<h3 className="text-base font-semibold leading-snug line-clamp-2 mb-2 group-hover:text-primary transition-colors">
					{event.name}
				</h3>

				<div className="flex flex-col gap-1.5 text-sm text-muted-foreground mb-4">
					<div className="flex items-center gap-2">
						<MapPinIcon className="h-3.5 w-3.5 shrink-0" />
						<span className="truncate">{event.venue}</span>
					</div>
					<div className="flex items-center gap-2">
						<CalendarIcon className="h-3.5 w-3.5 shrink-0" />
						<time dateTime={event.startsAt}>{formatDate(event.startsAt)}</time>
					</div>
				</div>

				{/* Footer */}
				<div className="mt-auto pt-4 border-t border-border/50">
					<div className="flex items-center justify-between">
						<div className="flex flex-col">
							<span className="text-lg font-bold">{formatPrice(event.price)}</span>
							{!isSoldOut && (
								<span className="text-xs text-muted-foreground">
									{event.available} entradas disponibles
								</span>
							)}
						</div>
						<span className="text-sm font-medium text-muted-foreground">
							Ver más →
						</span>
					</div>
				</div>
			</div>
		</article>
	);
}
