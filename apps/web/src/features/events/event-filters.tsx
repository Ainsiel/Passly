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
import { SearchIcon } from "lucide-react";

const CATEGORIES = [
	{ value: "", label: "Todas" },
	{ value: "CONCIERTO", label: "Concierto" },
	{ value: "TEATRO", label: "Teatro" },
	{ value: "EXPOSICION", label: "Exposición" },
	{ value: "DEPORTES", label: "Deportes" },
	{ value: "FESTIVAL", label: "Festival" },
	{ value: "CINE", label: "Cine" },
	{ value: "CONFERENCIA", label: "Conferencia" },
	{ value: "TALLER", label: "Taller" },
	{ value: "OTRA", label: "Otra" },
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
					placeholder="Busca eventos..."
					defaultValue={currentQ}
					onChange={(e) => updateParam("q", e.target.value)}
					className="pl-9"
					aria-label="Buscar eventos"
				/>
			</div>
			<Select
				defaultValue={currentCategory}
				onValueChange={(value: string | null) => updateParam("category", value ?? "")}
			>
				<SelectTrigger className="w-full sm:w-[180px]" aria-label="Filtrar por categoría">
					<SelectValue placeholder="Todas las categorías" />
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
				<span className="text-xs text-muted-foreground" aria-live="polite">
					Cargando...
				</span>
			)}
		</div>
	);
}
