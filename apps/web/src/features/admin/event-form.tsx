"use client";

import { useRouter } from "next/navigation";
import { useActionState, useEffect, useState } from "react";
import { createEvent, updateEvent, type AdminState } from "@/app/actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { LoaderIcon, SaveIcon, PlusIcon } from "lucide-react";
import type { EventDetail, EventCategory } from "@/lib/catalog";

const CATEGORIES: { value: EventCategory; label: string }[] = [
	{ value: "CONCIERTO", label: "Concierto" },
	{ value: "TEATRO", label: "Teatro" },
	{ value: "EXPOSICION", label: "Exposición" },
	{ value: "DEPORTE", label: "Deporte" },
	{ value: "FESTIVAL", label: "Festival" },
	{ value: "CINE", label: "Cine" },
	{ value: "CONFERENCIA", label: "Conferencia" },
	{ value: "TALLER", label: "Taller" },
];

interface EventFormProps {
	initialData?: EventDetail;
	action: typeof createEvent | typeof updateEvent;
	mode: "create" | "edit";
}

export function EventForm({ initialData, action, mode }: EventFormProps) {
	const router = useRouter();
	const [state, formAction, isPending] = useActionState<AdminState, FormData>(
		action,
		{ ok: false },
	);

	const [name, setName] = useState(initialData?.name ?? "");
	const [description, setDescription] = useState(initialData?.description ?? "");
	const [category, setCategory] = useState<EventCategory>(initialData?.category ?? "CONCIERTO");
	const [venue, setVenue] = useState(initialData?.venue ?? "");
	const [startsAt, setStartsAt] = useState(() => {
		if (initialData?.startsAt) {
			const d = new Date(initialData.startsAt);
			return d.toISOString().slice(0, 16);
		}
		return "";
	});
	const [price, setPrice] = useState(initialData?.price?.toString() ?? "0");
	const [capacity, setCapacity] = useState(initialData?.capacity?.toString() ?? "0");
	const [clientErrors, setClientErrors] = useState<Record<string, string>>({});

	useEffect(() => {
		if (state.ok) {
			router.push("/admin");
			router.refresh();
		}
	}, [state, router]);

	function validate(): boolean {
		const errors: Record<string, string> = {};

		if (!name.trim()) {
			errors.name = "El nombre es obligatorio";
		}
		if (!description.trim()) {
			errors.description = "La descripción es obligatoria";
		}
		if (!venue.trim()) {
			errors.venue = "El lugar es obligatorio";
		}
		if (!startsAt) {
			errors.startsAt = "La fecha y hora son obligatorias";
		} else if (new Date(startsAt) <= new Date()) {
			errors.startsAt = "La fecha debe ser en el futuro";
		}

		const priceNum = Number(price);
		if (isNaN(priceNum) || priceNum < 0) {
			errors.price = "El precio debe ser un número positivo";
		}

		const capacityNum = Number(capacity);
		if (isNaN(capacityNum) || capacityNum < 0) {
			errors.capacity = "La capacidad debe ser un número positivo";
		}

		setClientErrors(errors);
		return Object.keys(errors).length === 0;
	}

	function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
		if (!validate()) {
			e.preventDefault();
		}
	}

	return (
		<form
			action={formAction}
			onSubmit={handleSubmit}
			className="flex flex-col gap-6 max-w-2xl"
		>
			{initialData?.id && (
				<input type="hidden" name="id" value={initialData.id} />
			)}

			<div className="flex flex-col gap-2">
				<Label htmlFor="name">Nombre</Label>
				<Input
					id="name"
					name="name"
					value={name}
					onChange={(e) => setName(e.target.value)}
					placeholder="Nombre del evento"
				/>
				{clientErrors.name && (
					<p className="text-sm text-destructive">{clientErrors.name}</p>
				)}
			</div>

			<div className="flex flex-col gap-2">
				<Label htmlFor="description">Descripción</Label>
				<Textarea
					id="description"
					name="description"
					value={description}
					onChange={(e) => setDescription(e.target.value)}
					placeholder="Descripción del evento"
					style={{ minHeight: "100px" }}
				/>
				{clientErrors.description && (
					<p className="text-sm text-destructive">{clientErrors.description}</p>
				)}
			</div>

			<div className="flex flex-col gap-2">
				<Label>Categoría</Label>
				<Select
					name="category"
					value={category}
					onValueChange={(val) => setCategory(val as EventCategory)}
				>
					<SelectTrigger className="w-full">
						<SelectValue />
					</SelectTrigger>
					<SelectContent>
						{CATEGORIES.map((cat) => (
							<SelectItem key={cat.value} value={cat.value}>
								{cat.label}
							</SelectItem>
						))}
					</SelectContent>
				</Select>
			</div>

			<div className="flex flex-col gap-2">
				<Label htmlFor="venue">Lugar</Label>
				<Input
					id="venue"
					name="venue"
					value={venue}
					onChange={(e) => setVenue(e.target.value)}
					placeholder="Lugar del evento"
				/>
				{clientErrors.venue && (
					<p className="text-sm text-destructive">{clientErrors.venue}</p>
				)}
			</div>

			<div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
				<div className="flex flex-col gap-2">
					<Label htmlFor="startsAt">Fecha y hora</Label>
					<Input
						id="startsAt"
						name="startsAt"
						type="datetime-local"
						value={startsAt}
						onChange={(e) => setStartsAt(e.target.value)}
					/>
					{clientErrors.startsAt && (
						<p className="text-sm text-destructive">{clientErrors.startsAt}</p>
					)}
				</div>

				<div className="flex flex-col gap-2">
					<Label htmlFor="price">Precio (EUR)</Label>
					<Input
						id="price"
						name="price"
						type="number"
						min="0"
						step="0.01"
						value={price}
						onChange={(e) => setPrice(e.target.value)}
					/>
					{clientErrors.price && (
						<p className="text-sm text-destructive">{clientErrors.price}</p>
					)}
				</div>
			</div>

			<div className="flex flex-col gap-2">
				<Label htmlFor="capacity">Capacidad</Label>
				<Input
					id="capacity"
					name="capacity"
					type="number"
					min="0"
					value={capacity}
					onChange={(e) => setCapacity(e.target.value)}
				/>
				{clientErrors.capacity && (
					<p className="text-sm text-destructive">{clientErrors.capacity}</p>
				)}
			</div>

			{state.error && (
				<p className="text-sm text-destructive">{state.error}</p>
			)}

			<div className="flex gap-3">
				<Button
					type="submit"
					disabled={isPending}
					className="bg-foreground text-background hover:bg-foreground/90"
				>
					{isPending ? (
						<>
							<LoaderIcon className="h-4 w-4 mr-2 animate-spin" />
							{mode === "create" ? "Creando..." : "Guardando..."}
						</>
					) : mode === "create" ? (
						<>
							<PlusIcon className="h-4 w-4 mr-2" />
							Crear evento
						</>
					) : (
						<>
							<SaveIcon className="h-4 w-4 mr-2" />
							Guardar cambios
						</>
					)}
				</Button>
				<Button
					type="button"
					variant="outline"
					onClick={() => router.push("/admin")}
					disabled={isPending}
				>
					Cancelar
				</Button>
			</div>
		</form>
	);
}
