import type { Metadata } from "next";

import "./globals.css";

export const metadata: Metadata = {
	title: "Passly",
	description: "Reserva de tickets para eventos",
};

export default function RootLayout({
	children,
}: Readonly<{
	children: React.ReactNode;
}>) {
	return (
		<html lang="es">
			<body>{children}</body>
		</html>
	);
}
