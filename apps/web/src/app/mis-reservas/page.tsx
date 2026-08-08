import { auth } from "@/auth";
import { fetchMyReservations } from "@/lib/booking";
import { redirect } from "next/navigation";
import { Metadata } from "next";
import { ReservationList } from "@/features/reservations/reservation-list";

export const metadata: Metadata = {
	title: "Mis reservas — Passly",
};

export default async function MyReservationsPage() {
	const session = await auth();
	if (!session?.accessToken) redirect("/login");

	const reservations = await fetchMyReservations(session.accessToken);

	return (
		<div className="flex flex-col gap-6">
			<div>
				<h1 className="text-2xl font-bold tracking-tight">Mis reservas</h1>
				<p className="text-sm text-muted-foreground mt-1">
					Gestiona tus reservas y consulta los tickets de tus eventos.
				</p>
			</div>

			<ReservationList reservations={reservations} />
		</div>
	);
}
