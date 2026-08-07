import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { EventCategory } from "./catalog";

export function cn(...inputs: ClassValue[]) {
	return twMerge(clsx(inputs));
}

export function formatDate(iso: string): string {
	const date = new Date(iso);
	return date.toLocaleDateString("es-ES", {
		day: "numeric",
		month: "short",
		year: "numeric",
		hour: "2-digit",
		minute: "2-digit",
	});
}

export function formatPrice(price: number): string {
	if (price === 0) return "Gratuito";
	return new Intl.NumberFormat("es-ES", {
		style: "currency",
		currency: "EUR",
	}).format(price);
}

const CATEGORY_STYLES: Record<
	EventCategory,
	{ badge: string; label: string }
> = {
	CONCIERTO: {
		badge: "bg-concierto/15 text-concierto border-concierto/30",
		label: "Concierto",
	},
	TEATRO: {
		badge: "bg-teatro/15 text-teatro border-teatro/30",
		label: "Teatro",
	},
	EXPOSICION: {
		badge: "bg-exposicion/15 text-exposicion border-exposicion/30",
		label: "Exposición",
	},
	DEPORTES: {
		badge: "bg-deportes/15 text-deportes border-deportes/30",
		label: "Deportes",
	},
	OTRA: {
		badge: "bg-muted text-muted-foreground border-border",
		label: "Otra",
	},
	FESTIVAL: {
		badge: "bg-festival/15 text-festival border-festival/30",
		label: "Festival",
	},
	CINE: {
		badge: "bg-cine/15 text-cine border-cine/30",
		label: "Cine",
	},
	CONFERENCIA: {
		badge: "bg-conferencia/15 text-conferencia border-conferencia/30",
		label: "Conferencia",
	},
	TALLER: {
		badge: "bg-taller/15 text-taller border-taller/30",
		label: "Taller",
	},
};

export function categoryBadgeClass(category: EventCategory): string {
	return CATEGORY_STYLES[category]?.badge ?? CATEGORY_STYLES.OTRA.badge;
}

export function categoryLabel(category: EventCategory): string {
	return CATEGORY_STYLES[category]?.label ?? category;
}
