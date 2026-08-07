import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { EventFilters } from "@/features/events/event-filters";
import { EventList, EventListSkeleton } from "@/features/events/event-list";
import { fetchEvents, type EventCategory } from "@/lib/catalog";
import { categoryLabel } from "@/lib/utils";
import { SparklesIcon, CalendarIcon, MapPinIcon } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Passly — Explora eventos",
	description: "Encuentra y reserva tickets para los mejores eventos",
};

interface Props {
	searchParams: Promise<{
		q?: string;
		category?: string;
		page?: string;
		size?: string;
	}>;
}

export default async function HomePage({ searchParams }: Props) {
	const params = await searchParams;
	const page = params.page ? Number(params.page) : 0;
	const size = params.size ? Number(params.size) : 20;

	let content: Awaited<ReturnType<typeof fetchEvents>>["content"] = [];
	let meta: Awaited<ReturnType<typeof fetchEvents>>["page"] = {
		number: 0,
		size: 20,
		totalElements: 0,
		totalPages: 0,
	};

	try {
		const result = await fetchEvents({
			q: params.q,
			category: params.category,
			page,
			size,
		});
		content = result.content;
		meta = result.page;
	} catch {
		// API no disponible — mostrar estado vacío
	}

	const baseParams = new URLSearchParams();
	if (params.q) baseParams.set("q", params.q);
	if (params.category) baseParams.set("category", params.category);

	const hasActiveFilters = params.q || params.category;

	return (
		<div className="flex flex-col gap-8">
			{/* Hero Section */}
			<section className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-muted/50 via-muted/30 to-muted/50 border border-border/50 p-8 sm:p-12">
				<div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,oklch(0.7_0.05_250/0.15),transparent_50%)]" />
				<div className="relative">
					<div className="flex items-center gap-2 mb-4">
						<div className="flex h-8 w-8 items-center justify-center rounded-full bg-foreground/10">
							<SparklesIcon className="h-4 w-4 text-foreground" />
						</div>
						<span className="text-sm font-medium text-muted-foreground">Descubre eventos increíbles</span>
					</div>
					<h1 className="text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl mb-3">
						Encuentra tu próximo
						<br />
						<span className="bg-gradient-to-r from-foreground to-foreground/70 bg-clip-text text-transparent">
							evento favorito
						</span>
					</h1>
					<p className="text-muted-foreground text-lg max-w-xl">
						Explora conciertos, teatro, exposiciones y mucho más.
						Reserva tus tickets en segundos.
					</p>
					<div className="flex items-center gap-6 mt-6 text-sm text-muted-foreground">
						<div className="flex items-center gap-2">
							<CalendarIcon className="h-4 w-4" />
							<span>{meta.totalElements}+ eventos</span>
						</div>
						<div className="flex items-center gap-2">
							<MapPinIcon className="h-4 w-4" />
							<span>Múltiples sedes</span>
						</div>
					</div>
				</div>
			</section>

			{/* Filters */}
			<Suspense fallback={<EventFiltersSkeleton />}>
				<EventFilters />
			</Suspense>

			{/* Event List */}
			<section aria-label="Lista de eventos">
				{hasActiveFilters && (
					<div className="flex items-center gap-2 mb-4">
						<span className="text-sm text-muted-foreground">
							Mostrando resultados para:
						</span>
						{params.q && (
							<span className="inline-flex items-center gap-1 rounded-full bg-foreground/10 px-3 py-1 text-xs font-medium">
								&quot;{params.q}&quot;
							</span>
						)}
						{params.category && (
							<span className="inline-flex items-center gap-1 rounded-full bg-foreground/10 px-3 py-1 text-xs font-medium">
								{categoryLabel(params.category as EventCategory)}
							</span>
						)}
					</div>
				)}
				<Suspense fallback={<EventListSkeleton />}>
					<EventList events={content} page={meta} baseParams={baseParams.toString()} />
				</Suspense>
			</section>
		</div>
	);
}

function EventFiltersSkeleton() {
	return (
		<div className="flex gap-4">
			<Skeleton className="h-11 flex-1 rounded-xl" />
			<Skeleton className="h-11 w-48 rounded-xl" />
		</div>
	);
}
