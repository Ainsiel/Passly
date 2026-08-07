import Link from "next/link";
import { CalendarIcon, MapPinIcon, BanknoteIcon } from "lucide-react";
import {
	Breadcrumb,
	BreadcrumbItem,
	BreadcrumbLink,
	BreadcrumbList,
	BreadcrumbPage,
	BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Progress } from "@/components/ui/progress";
import { cn, formatDate, formatPrice, categoryBadgeClass, categoryLabel } from "@/lib/utils";
import type { EventDetail as EventDetailType } from "@/lib/catalog";

interface EventDetailProps {
	event: EventDetailType;
}

export function EventDetail({ event }: EventDetailProps) {
	const isSoldOut = event.available === 0;
	const spotsUsed = event.capacity - event.available;
	const percentage = event.capacity > 0 ? Math.round((spotsUsed / event.capacity) * 100) : 0;

	return (
		<div className="flex flex-col gap-6">
			<Breadcrumb>
				<BreadcrumbList>
					<BreadcrumbItem>
						<BreadcrumbLink href="/">Eventos</BreadcrumbLink>
					</BreadcrumbItem>
					<BreadcrumbSeparator />
					<BreadcrumbItem>
						<BreadcrumbLink href={`/?category=${event.category}`}>
							{categoryLabel(event.category)}
						</BreadcrumbLink>
					</BreadcrumbItem>
					<BreadcrumbSeparator />
					<BreadcrumbItem>
						<BreadcrumbPage>{event.name}</BreadcrumbPage>
					</BreadcrumbItem>
				</BreadcrumbList>
			</Breadcrumb>

			<div className="flex flex-col gap-4">
				<div className="flex flex-wrap items-start gap-3">
					<h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{event.name}</h1>
					<Badge
						variant="outline"
						className={cn("text-xs", categoryBadgeClass(event.category))}
					>
						{categoryLabel(event.category)}
					</Badge>
				</div>

				<div className="flex flex-col gap-2 text-sm text-muted-foreground">
					<div className="flex items-center gap-2">
						<CalendarIcon className="size-4" aria-hidden="true" />
						<time dateTime={event.startsAt}>{formatDate(event.startsAt)}</time>
					</div>
					<div className="flex items-center gap-2">
						<MapPinIcon className="size-4" aria-hidden="true" />
						<span>{event.venue}</span>
					</div>
					<div className="flex items-center gap-2">
						<BanknoteIcon className="size-4" aria-hidden="true" />
						<span className="font-medium text-foreground">{formatPrice(event.price)}</span>
					</div>
				</div>
			</div>

			<Separator />

			<section aria-labelledby="availability-heading" className="flex flex-col gap-3">
				<h2 id="availability-heading" className="text-sm font-medium">
					Disponibilidad
				</h2>
				<Progress value={percentage} aria-label={`${percentage}% de entradas vendidas`} />
				<div className="flex items-center justify-between text-sm">
					<span className="text-muted-foreground">
						{spotsUsed}/{event.capacity} entradas vendidas
					</span>
					{isSoldOut ? (
						<Badge variant="destructive">Agotado</Badge>
					) : (
						<Badge variant="secondary">
							Quedan {event.available} entradas
						</Badge>
					)}
				</div>
			</section>

			<Separator />

			<section aria-labelledby="description-heading" className="flex flex-col gap-3">
				<h2 id="description-heading" className="text-sm font-medium">
					Descripción
				</h2>
				<p className="text-sm leading-relaxed text-muted-foreground">
					{event.description}
				</p>
			</section>

			<div className="pt-2">
				<Link
					href="/"
					className="text-sm text-muted-foreground hover:text-foreground hover:underline"
				>
					← Volver a eventos
				</Link>
			</div>
		</div>
	);
}
