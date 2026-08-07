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
import { SearchIcon } from "lucide-react";
import type { EventSummary, PageMeta } from "@/lib/catalog";

interface EventListProps {
	events: EventSummary[];
	page: PageMeta;
	baseParams?: string;
}

export function EventList({ events, page, baseParams = "" }: EventListProps) {
	if (events.length === 0) {
		return (
			<div className="flex flex-col items-center justify-center py-16 text-center">
				<div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted mb-4">
					<SearchIcon className="h-8 w-8 text-muted-foreground" />
				</div>
				<h3 className="text-lg font-semibold mb-1">No se encontraron eventos</h3>
				<p className="text-sm text-muted-foreground max-w-sm">
					Intenta ajustar los filtros de búsqueda o explora otras categorías
				</p>
			</div>
		);
	}

	const params = baseParams ? `${baseParams}&` : "?";

	return (
		<div className="flex flex-col gap-6">
			<div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
				{events.map((event, index) => (
					<div
						key={event.id}
						className="animate-in fade-in slide-up"
						style={{ animationDelay: `${index * 50}ms` }}
					>
						<EventCard event={event} />
					</div>
				))}
			</div>

			{page.totalPages > 1 && (
				<nav aria-label="Paginación de eventos" className="flex justify-center pt-4">
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
		<div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
			{Array.from({ length: 8 }).map((_, i) => (
				<div key={i} className="flex flex-col rounded-xl border border-border/60 bg-card overflow-hidden">
					<Skeleton className="h-32 sm:h-36 rounded-none" />
					<div className="flex flex-col gap-3 p-4 sm:p-5">
						<Skeleton className="h-5 w-3/4" />
						<Skeleton className="h-4 w-1/2" />
						<div className="mt-4 pt-4 border-t border-border/50 flex justify-between">
							<Skeleton className="h-6 w-16" />
							<Skeleton className="h-4 w-20" />
						</div>
					</div>
				</div>
			))}
		</div>
	);
}
