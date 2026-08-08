"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useActionState, useEffect, useMemo } from "react";
import { reserveTickets, type ReserveState } from "@/app/actions";
import { Button } from "@/components/ui/button";
import { TicketIcon, LoaderIcon } from "lucide-react";

interface ReserveButtonProps {
	isLoggedIn: boolean;
	isSoldOut: boolean;
	eventId: number;
}

export function ReserveButton({ isLoggedIn, isSoldOut, eventId }: ReserveButtonProps) {
	const router = useRouter();
	const [state, formAction, isPending] = useActionState<ReserveState, FormData>(
		reserveTickets,
		{ ok: false },
	);

	const idempotencyKey = useMemo(() => crypto.randomUUID(), []);

	useEffect(() => {
		if (state.ok && state.reservationId) {
			router.push(`/reservas/${state.reservationId}/tickets`);
		}
	}, [state, router]);

	if (isSoldOut) {
		return (
			<Button className="w-full h-11" disabled>
				<TicketIcon className="h-4 w-4 mr-2" />
				Agotado
			</Button>
		);
	}

	if (!isLoggedIn) {
		return (
			<Button
				render={<Link href="/login" />}
				className="w-full bg-foreground text-background hover:bg-foreground/90 h-11"
			>
				<TicketIcon className="h-4 w-4 mr-2" />
				Inicia sesión para reservar
			</Button>
		);
	}

	return (
		<form action={formAction} className="flex flex-col gap-3">
			<input type="hidden" name="eventId" value={eventId} />
			<input type="hidden" name="idempotencyKey" value={idempotencyKey} />

			<div className="flex flex-col gap-1.5">
				<label htmlFor="quantity" className="text-sm font-medium">
					Cantidad
				</label>
				<select
					id="quantity"
					name="quantity"
					defaultValue="1"
					className="flex h-10 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
				>
					<option value="1">1 ticket</option>
					<option value="2">2 tickets</option>
					<option value="3">3 tickets</option>
					<option value="4">4 tickets</option>
				</select>
			</div>

			<Button
				type="submit"
				disabled={isPending}
				className="w-full bg-foreground text-background hover:bg-foreground/90 h-11"
			>
				{isPending ? (
					<>
						<LoaderIcon className="h-4 w-4 mr-2 animate-spin" />
						Reservando...
					</>
				) : (
					<>
						<TicketIcon className="h-4 w-4 mr-2" />
						Reservar entradas
					</>
				)}
			</Button>

			{state.error && (
				<p className="text-sm text-destructive text-center">{state.error}</p>
			)}
		</form>
	);
}
