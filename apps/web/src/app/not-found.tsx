import Link from "next/link";
import { Button } from "@/components/ui/button";
import { SearchIcon, ArrowLeftIcon } from "lucide-react";

export default function NotFound() {
	return (
		<div className="flex min-h-[60vh] items-center justify-center">
			<div className="flex flex-col items-center text-center">
				<div className="flex h-20 w-20 items-center justify-center rounded-full bg-muted mb-6">
					<SearchIcon className="h-10 w-10 text-muted-foreground" />
				</div>
				<h1 className="text-4xl font-bold tracking-tight mb-2">404</h1>
				<h2 className="text-xl font-semibold mb-2">Página no encontrada</h2>
				<p className="text-muted-foreground max-w-md mb-8">
					La página que buscas no existe o ha sido movida a otra ubicación.
				</p>
				<Button render={<Link href="/" />} className="bg-foreground text-background hover:bg-foreground/90">
					<ArrowLeftIcon className="h-4 w-4 mr-2" />
					Volver al inicio
				</Button>
			</div>
		</div>
	);
}
