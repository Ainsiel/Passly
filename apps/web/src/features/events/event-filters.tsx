"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";
import { Input } from "@/components/ui/input";
import {
	Select,
	SelectContent,
	SelectGroup,
	SelectItem,
	SelectLabel,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { SearchIcon, Loader2Icon } from "lucide-react";

const CATEGORIES = [
	{ value: "", label: "Todas" },
	{ value: "CONCIERTO", label: "Concierto" },
	{ value: "TEATRO", label: "Teatro" },
	{ value: "EXPOSICION", label: "Exposición" },
	{ value: "DEPORTE", label: "Deporte" },
	{ value: "FESTIVAL", label: "Festival" },
	{ value: "CINE", label: "Cine" },
	{ value: "CONFERENCIA", label: "Conferencia" },
	{ value: "TALLER", label: "Taller" },
];

export function EventFilters() {
	const router = useRouter();
	const searchParams = useSearchParams();
	const [isPending, startTransition] = useTransition();

	const currentQ = searchParams.get("q") ?? "";
	const currentCategory = searchParams.get("category") ?? "";

	function updateParam(key: string, value: string) {
		const params = new URLSearchParams(searchParams.toString());
		if (value) {
			params.set(key, value);
		} else {
			params.delete(key);
		}
		params.delete("page");
		startTransition(() => {
			router.push(`/?${params.toString()}`);
		});
	}

	return (
		<div className="flex flex-col gap-3 sm:flex-row sm:items-center">
			<div className="relative flex-1">
				<SearchIcon
					className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
					aria-hidden="true"
				/>
				<Input
					placeholder="Buscar eventos..."
					defaultValue={currentQ}
					onChange={(e) => updateParam("q", e.target.value)}
					className="pl-9 h-11 bg-muted/50 border-border/60 focus:bg-background transition-colors"
					aria-label="Buscar eventos"
				/>
			</div>
			<Select
				defaultValue={currentCategory}
				onValueChange={(value: string | null) => updateParam("category", value ?? "")}
			>
				<SelectTrigger className="w-full sm:w-[180px] h-11 bg-muted/50 border-border/60" aria-label="Filtrar por categoría">
					<SelectValue placeholder="Todas las categorías">
					{(value: string | null) => {
						if (!value) return "Todas las categorías";
						return CATEGORIES.find(c => c.value === value)?.label ?? "Todas las categorías";
					}}
				</SelectValue>
				</SelectTrigger>
				<SelectContent>
					<SelectGroup>
						<SelectLabel>Categoría</SelectLabel>
						{CATEGORIES.map((cat) => (
							<SelectItem key={cat.value} value={cat.value}>
								{cat.label}
							</SelectItem>
						))}
					</SelectGroup>
				</SelectContent>
			</Select>
			{isPending && (
				<div className="flex items-center gap-2 text-xs text-muted-foreground" aria-live="polite">
					<Loader2Icon className="h-3 w-3 animate-spin" />
					<span>Cargando...</span>
				</div>
			)}
		</div>
	);
}
