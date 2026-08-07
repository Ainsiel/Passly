"use client";

import { useTheme } from "next-themes";
import { Button } from "@/components/ui/button";
import { MoonIcon, SunIcon } from "lucide-react";
import { useEffect, useState } from "react";

export function ThemeToggle() {
	const { theme, setTheme } = useTheme();
	const [mounted, setMounted] = useState(false);

	useEffect(() => {
		setMounted(true);
	}, []);

	if (!mounted) {
		return (
			<Button variant="ghost" size="icon" className="h-9 w-9">
				<SunIcon className="h-4 w-4" />
			</Button>
		);
	}

	return (
		<Button
			variant="ghost"
			size="icon"
			className="h-9 w-9"
			onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
			aria-label={`Cambiar a modo ${theme === "dark" ? "claro" : "oscuro"}`}
		>
			{theme === "dark" ? (
				<SunIcon className="h-4 w-4 transition-transform" />
			) : (
				<MoonIcon className="h-4 w-4 transition-transform" />
			)}
		</Button>
	);
}
