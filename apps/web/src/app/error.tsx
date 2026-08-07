"use client";

import { Button } from "@/components/ui/button";
import { AlertTriangleIcon, RefreshCwIcon } from "lucide-react";

export default function Error({
	error,
	reset,
}: {
	error: Error & { digest?: string };
	reset: () => void;
}) {
	return (
		<div className="flex min-h-[60vh] items-center justify-center">
			<div className="flex flex-col items-center text-center">
				<div className="flex h-20 w-20 items-center justify-center rounded-full bg-destructive/10 mb-6">
					<AlertTriangleIcon className="h-10 w-10 text-destructive" />
				</div>
				<h1 className="text-4xl font-bold tracking-tight mb-2">Error</h1>
				<h2 className="text-xl font-semibold mb-2">Algo salió mal</h2>
				<p className="text-muted-foreground max-w-md mb-8">
					{error.message || "Ha ocurrido un error inesperado. Por favor, inténtalo de nuevo."}
				</p>
				<Button onClick={reset} className="bg-foreground text-background hover:bg-foreground/90">
					<RefreshCwIcon className="h-4 w-4 mr-2" />
					Reintentar
				</Button>
			</div>
		</div>
	);
}
