import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { fetchEvents } from "@/lib/catalog";
import { AdminEventList } from "@/features/admin/event-list";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Admin — Passly",
	description: "Gestión de eventos",
};

interface Props {
	searchParams: Promise<{ page?: string }>;
}

export default async function AdminPage({ searchParams }: Props) {
	const session = await auth();
	if (!session?.user) redirect("/login");
	if (!session.user.roles?.includes("ADMIN")) redirect("/");

	const params = await searchParams;
	const page = params.page ? Number(params.page) : 0;

	const result = await fetchEvents({ page, size: 20 });

	return (
		<AdminEventList
			events={result.content}
			page={result.page}
		/>
	);
}
