"use client";

import { useActionState, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { deleteEvent, type AdminState } from "@/app/actions";
import { Button } from "@/components/ui/button";
import {
	Dialog,
	DialogContent,
	DialogDescription,
	DialogFooter,
	DialogHeader,
	DialogTitle,
	DialogTrigger,
} from "@/components/ui/dialog";
import { TrashIcon, LoaderIcon } from "lucide-react";

interface DeleteEventDialogProps {
	eventId: string;
	eventName: string;
	onSuccess?: () => void;
}

export function DeleteEventDialog({
	eventId,
	eventName,
	onSuccess,
}: DeleteEventDialogProps) {
	const router = useRouter();
	const [open, setOpen] = useState(false);
	const [state, formAction, isPending] = useActionState<AdminState, FormData>(
		deleteEvent,
		{ ok: false },
	);

	useEffect(() => {
		if (state.ok) {
			setOpen(false);
			router.refresh();
			onSuccess?.();
		}
	}, [state, router, onSuccess]);

	return (
		<Dialog open={open} onOpenChange={setOpen}>
			<DialogTrigger
				render={
					<Button
						variant="ghost"
						size="icon-sm"
						className="text-destructive hover:text-destructive hover:bg-destructive/10"
					/>
				}
			>
				<TrashIcon className="h-4 w-4" />
				<span className="sr-only">Eliminar {eventName}</span>
			</DialogTrigger>
			<DialogContent>
				<DialogHeader>
					<DialogTitle>Eliminar evento</DialogTitle>
					<DialogDescription>
						¿Estás seguro de que deseas eliminar &quot;{eventName}&quot;? Esta acción no
						se puede deshacer.
					</DialogDescription>
				</DialogHeader>
				{state.error && (
					<p className="text-sm text-destructive">{state.error}</p>
				)}
				<DialogFooter>
					<form action={formAction} className="contents">
						<input type="hidden" name="id" value={eventId} />
						<Button
							type="button"
							variant="outline"
							onClick={() => setOpen(false)}
							disabled={isPending}
						>
							Cancelar
						</Button>
						<Button
							type="submit"
							variant="destructive"
							disabled={isPending}
						>
							{isPending ? (
								<>
									<LoaderIcon className="h-4 w-4 mr-2 animate-spin" />
									Eliminando...
								</>
							) : (
								<>
									<TrashIcon className="h-4 w-4 mr-2" />
									Eliminar
								</>
							)}
						</Button>
					</form>
				</DialogFooter>
			</DialogContent>
		</Dialog>
	);
}
