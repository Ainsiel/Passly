"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { TicketIcon } from "lucide-react";

interface ReserveButtonProps {
	isLoggedIn: boolean;
	isSoldOut: boolean;
}

export function ReserveButton({ isLoggedIn, isSoldOut }: ReserveButtonProps) {
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
				render={
					<Link href="/login" />
				}
				className="w-full bg-foreground text-background hover:bg-foreground/90 h-11"
			>
				<TicketIcon className="h-4 w-4 mr-2" />
				Inicia sesión para reservar
			</Button>
		);
	}

	return (
		<Button
			className="w-full bg-foreground text-background hover:bg-foreground/90 h-11"
		>
			<TicketIcon className="h-4 w-4 mr-2" />
			Reservar entradas
		</Button>
	);
}
