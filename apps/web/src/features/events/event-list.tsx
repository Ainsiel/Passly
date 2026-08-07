"use client";

import { EventCard } from "./event-card";
import { Skeleton } from "@/components/ui/skeleton";
import {
	Pagination,
	PaginationContent,
	PaginationItem,
	PaginationLink,
	PaginationNext,
	PaginationPrevious,
} from "@/components/ui/pagination";
import type { EventSummary, PageMeta } from "@/lib/catalog";

interface EventListProps {
	events: EventSummary[];
	page: PageMeta;
	baseParams?: string;
}

export function EventList({ events, page, baseParams = "" }: EventListProps) {
	if (events.length === 0) {
		return (
			<div className="py-12 text-center">
				<p className="text-lg text-muted-foreground">No se encontraron eventos</p>
				<p className="text-sm text-muted-foreground">
					Intenta ajustar los filtros de búsqueda
				</p>
			</div>
		);
	}

	const params = baseParams ? `${baseParams}&` : "?";

	return (
		<div className="flex flex-col gap-6">
			<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
				{events.map((event) => (
					<EventCard key={event.id} event={event} />
				))}
			</div>

			{page.totalPages > 1 && (
				<nav aria-label="Paginación de eventos" className="flex justify-center">
					<Pagination>
						<PaginationContent>
							<PaginationItem>
								<PaginationPrevious
									href={`${params}page=${page.number - 1}`}
									aria-disabled={page.number === 0}
									tabIndex={page.number === 0 ? -1 : undefined}
									className={page.number === 0 ? "pointer-events-none opacity-50" : ""}
								/>
							</PaginationItem>
							{Array.from({ length: page.totalPages }, (_, i) => (
								<PaginationItem key={i}>
									<PaginationLink
										href={`${params}page=${i}`}
										isActive={i === page.number}
										aria-label={`Página ${i + 1}`}
									>
										{i + 1}
									</PaginationLink>
								</PaginationItem>
							))}
							<PaginationItem>
								<PaginationNext
									href={`${params}page=${page.number + 1}`}
									aria-disabled={page.number === page.totalPages - 1}
									tabIndex={page.number === page.totalPages - 1 ? -1 : undefined}
									className={
										page.number === page.totalPages - 1
											? "pointer-events-none opacity-50"
											: ""
									}
								/>
							</PaginationItem>
						</PaginationContent>
					</Pagination>
				</nav>
			)}
		</div>
	);
}

export function EventListSkeleton() {
	return (
		<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
			{Array.from({ length: 8 }).map((_, i) => (
				<Skeleton key={i} className="h-[220px] rounded-xl" />
			))}
		</div>
	);
}
