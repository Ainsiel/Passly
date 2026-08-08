"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "@/components/ui/table";
import {
	Pagination,
	PaginationContent,
	PaginationItem,
	PaginationLink,
	PaginationNext,
	PaginationPrevious,
} from "@/components/ui/pagination";
import { Badge } from "@/components/ui/badge";
import { PencilIcon, PlusIcon } from "lucide-react";
import { formatDate, formatPrice, categoryLabel, categoryBadgeClass } from "@/lib/utils";
import { DeleteEventDialog } from "./delete-event-dialog";
import type { EventSummary, PageMeta } from "@/lib/catalog";

interface AdminEventListProps {
	events: EventSummary[];
	page: PageMeta;
}

export function AdminEventList({ events, page }: AdminEventListProps) {
	if (events.length === 0) {
		return (
			<div className="flex flex-col items-center justify-center py-16 text-center">
				<h3 className="text-lg font-semibold mb-1">No hay eventos</h3>
				<p className="text-sm text-muted-foreground mb-4">
					Crea tu primer evento para comenzar
				</p>
				<Button render={<Link href="/admin/nuevo" />} className="gap-2">
					<PlusIcon className="h-4 w-4" />
					Crear evento
				</Button>
			</div>
		);
	}

	return (
		<div className="flex flex-col gap-4">
			<div className="flex justify-end">
				<Button render={<Link href="/admin/nuevo" />} className="gap-2">
					<PlusIcon className="h-4 w-4" />
					Crear evento
				</Button>
			</div>

			<div className="rounded-lg border">
				<Table>
					<TableHeader>
						<TableRow>
							<TableHead>Nombre</TableHead>
							<TableHead>Categoría</TableHead>
							<TableHead className="hidden sm:table-cell">Lugar</TableHead>
							<TableHead className="hidden md:table-cell">Fecha</TableHead>
							<TableHead className="text-right">Precio</TableHead>
							<TableHead className="text-right">Disponibilidad</TableHead>
							<TableHead className="text-right">Acciones</TableHead>
						</TableRow>
					</TableHeader>
					<TableBody>
						{events.map((event) => (
							<TableRow key={event.id}>
								<TableCell className="font-medium max-w-[200px] truncate">
									{event.name}
								</TableCell>
								<TableCell>
									<Badge
										variant="secondary"
										className={categoryBadgeClass(event.category)}
									>
										{categoryLabel(event.category)}
									</Badge>
								</TableCell>
								<TableCell className="hidden sm:table-cell max-w-[150px] truncate text-muted-foreground">
									{event.venue}
								</TableCell>
								<TableCell className="hidden md:table-cell text-muted-foreground">
									{formatDate(event.startsAt)}
								</TableCell>
								<TableCell className="text-right font-medium">
									{formatPrice(event.price)}
								</TableCell>
								<TableCell className="text-right">
									<span
										className={
											event.available > 0
												? "text-muted-foreground"
												: "text-destructive font-medium"
										}
									>
										{event.available > 0
											? `${event.available} disponibles`
											: "Agotado"}
									</span>
								</TableCell>
								<TableCell className="text-right">
									<div className="flex items-center justify-end gap-1">
										<Button
											variant="ghost"
											size="icon-sm"
											render={<Link href={"/admin/" + event.id + "/editar"} />}
										>
											<PencilIcon className="h-4 w-4" />
											<span className="sr-only">Editar {event.name}</span>
										</Button>
										<DeleteEventDialog
											eventId={event.id}
											eventName={event.name}
										/>
									</div>
								</TableCell>
							</TableRow>
						))}
					</TableBody>
				</Table>
			</div>

			{page.totalPages > 1 && (
				<nav aria-label="Paginación de eventos" className="flex justify-center pt-2">
					<Pagination>
						<PaginationContent>
							<PaginationItem>
								<PaginationPrevious
									href={`/admin?page=${page.number - 1}`}
									aria-disabled={page.number === 0}
									tabIndex={page.number === 0 ? -1 : undefined}
									className={page.number === 0 ? "pointer-events-none opacity-50" : ""}
								/>
							</PaginationItem>
							{Array.from({ length: page.totalPages }, (_, i) => (
								<PaginationItem key={i}>
									<PaginationLink
										href={`/admin?page=${i}`}
										isActive={i === page.number}
										aria-label={`Página ${i + 1}`}
									>
										{i + 1}
									</PaginationLink>
								</PaginationItem>
							))}
							<PaginationItem>
								<PaginationNext
									href={`/admin?page=${page.number + 1}`}
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
