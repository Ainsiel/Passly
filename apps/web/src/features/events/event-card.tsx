import Link from "next/link";
import { CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn, formatDate, formatPrice, categoryBadgeClass, categoryLabel } from "@/lib/utils";
import type { EventSummary } from "@/lib/catalog";

interface EventCardProps {
	event: EventSummary;
}

export function EventCard({ event }: EventCardProps) {
	const isSoldOut = event.available === 0;

	return (
		<article
			className={cn(
				"flex flex-col rounded-xl border border-border bg-card text-card-foreground transition-colors hover:bg-accent/50",
				isSoldOut && "opacity-60"
			)}
			aria-label={`${event.name}, ${categoryLabel(event.category)}`}
		>
			<CardHeader className="pb-3">
				<div className="flex items-start justify-between gap-2">
					<CardTitle className="text-base leading-snug">{event.name}</CardTitle>
					<Badge
						variant="outline"
						className={cn("shrink-0 text-xs", categoryBadgeClass(event.category))}
					>
						{categoryLabel(event.category)}
					</Badge>
				</div>
				<CardDescription className="text-sm">{event.venue}</CardDescription>
			</CardHeader>

			<CardFooter className="mt-auto flex flex-col gap-3 pt-0">
				<div className="flex w-full items-center justify-between text-sm">
					<time dateTime={event.startsAt} className="text-muted-foreground">
						{formatDate(event.startsAt)}
					</time>
					<span className="font-medium">{formatPrice(event.price)}</span>
				</div>

				<div className="flex w-full items-center justify-between">
					{isSoldOut ? (
						<Badge variant="destructive">Agotado</Badge>
					) : (
						<span className="text-xs text-muted-foreground">
							Quedan {event.available} entradas
						</span>
					)}
				<Button
					render={<Link href={`/eventos/${event.id}`} aria-label={`Ver detalle de ${event.name}`} />}
					size="sm"
					variant="outline"
				>
					Ver detalle
				</Button>
				</div>
			</CardFooter>
		</article>
	);
}
