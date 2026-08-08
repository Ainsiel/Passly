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
	{ badge: string; label: string; gradient: string }
> = {
	CONCIERTO: {
		badge: "bg-concierto/15 text-concierto border-concierto/30",
		label: "Concierto",
		gradient: "from-blue-600 to-blue-800",
	},
	TEATRO: {
		badge: "bg-teatro/15 text-teatro border-teatro/30",
		label: "Teatro",
		gradient: "from-pink-600 to-rose-800",
	},
	EXPOSICION: {
		badge: "bg-exposicion/15 text-exposicion border-exposicion/30",
		label: "Exposición",
		gradient: "from-teal-600 to-emerald-800",
	},
	DEPORTE: {
		badge: "bg-deportes/15 text-deportes border-deportes/30",
		label: "Deporte",
		gradient: "from-orange-500 to-amber-700",
	},
	FESTIVAL: {
		badge: "bg-festival/15 text-festival border-festival/30",
		label: "Festival",
		gradient: "from-purple-600 to-violet-800",
	},
	CINE: {
		badge: "bg-cine/15 text-cine border-cine/30",
		label: "Cine",
		gradient: "from-red-500 to-orange-700",
	},
	CONFERENCIA: {
		badge: "bg-conferencia/15 text-conferencia border-conferencia/30",
		label: "Conferencia",
		gradient: "from-slate-600 to-slate-800",
	},
	TALLER: {
		badge: "bg-taller/15 text-taller border-taller/30",
		label: "Taller",
		gradient: "from-lime-600 to-green-800",
	},
};

const DEFAULT_STYLE = {
	badge: "bg-muted text-muted-foreground border-border",
	label: "",
	gradient: "from-gray-500 to-gray-700",
};

export function categoryBadgeClass(category: EventCategory): string {
	return CATEGORY_STYLES[category]?.badge ?? DEFAULT_STYLE.badge;
}

export function categoryLabel(category: EventCategory): string {
	return CATEGORY_STYLES[category]?.label ?? category;
}

export function categoryGradientClass(category: EventCategory): string {
	return CATEGORY_STYLES[category]?.gradient ?? DEFAULT_STYLE.gradient;
}
