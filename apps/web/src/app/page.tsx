import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { EventFilters } from "@/features/events/event-filters";
import { EventList, EventListSkeleton } from "@/features/events/event-list";
import { fetchEvents } from "@/lib/catalog";
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
		console.log("[HomePage] fetchEvents OK:", content.length, "events");
	} catch (e) {
		// API no disponible — mostrar estado vacío
		console.error("[HomePage] fetchEvents failed:", e);
	}

	const baseParams = new URLSearchParams();
	if (params.q) baseParams.set("q", params.q);
	if (params.category) baseParams.set("category", params.category);

	return (
		<div className="flex flex-col gap-6">
			<div>
				<h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
					Explora eventos
				</h1>
				<p className="text-muted-foreground">
					Descubre conciertos, teatro, exposiciones y más
				</p>
			</div>

			<Suspense fallback={<EventFiltersSkeleton />}>
				<EventFilters />
			</Suspense>

			<section aria-label="Lista de eventos">
				<Suspense fallback={<EventListSkeleton />}>
					<EventList events={content} page={meta} baseParams={baseParams.toString()} />
				</Suspense>
			</section>
		</div>
	);
}

function EventFiltersSkeleton() {
	return <Skeleton className="h-10 w-full rounded-md" />;
}
