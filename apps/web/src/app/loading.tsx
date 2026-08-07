import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
	return (
		<div className="flex flex-col gap-8">
			{/* Hero skeleton */}
			<div className="rounded-2xl bg-muted/50 border border-border/50 p-8 sm:p-12">
				<Skeleton className="h-6 w-48 mb-4" />
				<Skeleton className="h-12 w-96 mb-3" />
				<Skeleton className="h-5 w-80 mb-6" />
				<div className="flex gap-6">
					<Skeleton className="h-4 w-24" />
					<Skeleton className="h-4 w-24" />
				</div>
			</div>

			{/* Filters skeleton */}
			<div className="flex gap-4">
				<Skeleton className="h-11 flex-1 rounded-xl" />
				<Skeleton className="h-11 w-48 rounded-xl" />
			</div>

			{/* Grid skeleton */}
			<div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
				{Array.from({ length: 8 }).map((_, i) => (
					<div key={i} className="flex flex-col rounded-xl border border-border/60 bg-card overflow-hidden">
						<Skeleton className="h-32 sm:h-36 rounded-none" />
						<div className="flex flex-col gap-3 p-4 sm:p-5">
							<Skeleton className="h-5 w-3/4" />
							<Skeleton className="h-4 w-1/2" />
							<div className="mt-4 pt-4 border-t border-border/50 flex justify-between">
								<Skeleton className="h-6 w-16" />
								<Skeleton className="h-4 w-20" />
							</div>
						</div>
					</div>
				))}
			</div>
		</div>
	);
}
