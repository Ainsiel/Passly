import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { ThemeProvider } from "next-themes";
import { Header } from "@/components/header";
import "./globals.css";

const inter = Inter({
	subsets: ["latin"],
	variable: "--font-inter",
	display: "swap",
});

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
		<html lang="es" suppressHydrationWarning>
			<body className={`${inter.variable} font-sans antialiased`}>
				<ThemeProvider
					attribute="class"
					defaultTheme="system"
					enableSystem
					disableTransitionOnChange
				>
					<a
						href="#main-content"
						className="absolute -left-[9999px] -top-[9999px] z-50 bg-background p-2 focus:left-0 focus:top-0 focus:border focus:border-border"
					>
						Saltar al contenido principal
					</a>
					<Header />
					<main id="main-content" className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
						{children}
					</main>
				</ThemeProvider>
			</body>
		</html>
	);
}
