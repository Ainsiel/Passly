import Link from "next/link";
import { auth } from "@/auth";
import { CalendarIcon, MapPinIcon, BanknoteIcon, UsersIcon, ArrowLeftIcon } from "lucide-react";
import {
	Breadcrumb,
	BreadcrumbItem,
	BreadcrumbLink,
	BreadcrumbList,
	BreadcrumbPage,
	BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { cn, formatDate, formatPrice, categoryLabel, categoryGradientClass } from "@/lib/utils";
import { ReserveButton } from "./event-detail-reserve";
import type { EventDetail as EventDetailType } from "@/lib/catalog";

interface EventDetailProps {
	event: EventDetailType;
}

export async function EventDetail({ event }: EventDetailProps) {
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

			{/* Hero Section */}
			<section className={cn(
				"relative overflow-hidden rounded-2xl bg-gradient-to-br p-8 sm:p-10",
				categoryGradientClass(event.category)
			)}>
				<div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_30%,rgba(255,255,255,0.15),transparent_60%)]" />
				<div className="relative">
					<Badge
						variant="secondary"
						className="bg-white/20 text-white border-white/30 backdrop-blur-sm mb-4"
					>
						{categoryLabel(event.category)}
					</Badge>
					<h1 className="text-3xl font-bold tracking-tight sm:text-4xl text-white mb-2">
						{event.name}
					</h1>
					<div className="flex flex-wrap items-center gap-4 text-white/80 text-sm">
						<div className="flex items-center gap-2">
							<CalendarIcon className="h-4 w-4" />
							<time dateTime={event.startsAt}>{formatDate(event.startsAt)}</time>
						</div>
						<div className="flex items-center gap-2">
							<MapPinIcon className="h-4 w-4" />
							<span>{event.venue}</span>
						</div>
					</div>
				</div>
			</section>

			{/* Content Grid */}
			<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
				{/* Main Content */}
				<div className="lg:col-span-2 flex flex-col gap-6">
					<section aria-labelledby="description-heading" className="rounded-xl border border-border/60 bg-card p-6">
						<h2 id="description-heading" className="text-lg font-semibold mb-4">
							Sobre el evento
						</h2>
						<p className="text-muted-foreground leading-relaxed">
							{event.description}
						</p>
					</section>

					<section aria-labelledby="details-heading" className="rounded-xl border border-border/60 bg-card p-6">
						<h2 id="details-heading" className="text-lg font-semibold mb-4">
							Detalles
						</h2>
						<div className="grid grid-cols-2 gap-4">
							<div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
								<div className="flex h-10 w-10 items-center justify-center rounded-lg bg-background">
									<CalendarIcon className="h-5 w-5 text-muted-foreground" />
								</div>
								<div>
									<p className="text-sm font-medium">Fecha</p>
									<p className="text-sm text-muted-foreground">{formatDate(event.startsAt)}</p>
								</div>
							</div>
							<div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
								<div className="flex h-10 w-10 items-center justify-center rounded-lg bg-background">
									<MapPinIcon className="h-5 w-5 text-muted-foreground" />
								</div>
								<div>
									<p className="text-sm font-medium">Sede</p>
									<p className="text-sm text-muted-foreground">{event.venue}</p>
								</div>
							</div>
							<div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
								<div className="flex h-10 w-10 items-center justify-center rounded-lg bg-background">
									<BanknoteIcon className="h-5 w-5 text-muted-foreground" />
								</div>
								<div>
									<p className="text-sm font-medium">Precio</p>
									<p className="text-sm text-muted-foreground">{formatPrice(event.price)}</p>
								</div>
							</div>
							<div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50">
								<div className="flex h-10 w-10 items-center justify-center rounded-lg bg-background">
									<UsersIcon className="h-5 w-5 text-muted-foreground" />
								</div>
								<div>
									<p className="text-sm font-medium">Capacidad</p>
									<p className="text-sm text-muted-foreground">{event.capacity} personas</p>
								</div>
							</div>
						</div>
					</section>
				</div>

				{/* Sidebar */}
				<div className="lg:col-span-1">
					<div className="sticky top-24 rounded-xl border border-border/60 bg-card p-6 flex flex-col gap-6">
						{/* Price */}
						<div className="text-center">
							<p className="text-sm text-muted-foreground mb-1">Precio</p>
							<p className="text-3xl font-bold">{formatPrice(event.price)}</p>
						</div>

						{/* Availability */}
						<section aria-labelledby="availability-heading" className="flex flex-col gap-3">
							<h2 id="availability-heading" className="text-sm font-medium">
								Disponibilidad
							</h2>
							<Progress value={percentage} aria-label={`${percentage}% de entradas vendidas`} className="h-2" />
							<div className="flex items-center justify-between text-sm">
								<span className="text-muted-foreground">
									{spotsUsed}/{event.capacity} vendidas
								</span>
								{isSoldOut ? (
									<Badge variant="destructive">Agotado</Badge>
								) : (
									<Badge variant="secondary" className="bg-green-500/10 text-green-600 border-green-500/20">
										{event.available} disponibles
									</Badge>
								)}
							</div>
						</section>

						{/* CTA */}
						<ReserveButton
							isLoggedIn={!!await auth()}
							isSoldOut={isSoldOut}
							eventId={Number(event.id)}
						/>

						{/* Back Link */}
						<Link
							href="/"
							className="flex items-center justify-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
						>
							<ArrowLeftIcon className="h-4 w-4" />
							Volver a eventos
						</Link>
					</div>
				</div>
			</div>
		</div>
	);
}
