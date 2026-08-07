import { expect, test } from "@playwright/test";

test.describe("Auth de punta a punta (ticket #3)", () => {
	test("usuario anónimo: ve formulario de login", async ({ page }) => {
		await page.goto("/");
		await expect(page.getByRole("button", { name: /Menú de usuario/ })).toBeHidden();
		await expect(page.getByRole("link", { name: /Iniciar sesión/ })).toBeVisible();
	});

	test("login con formulario: queda logueado en home", async ({ page }) => {
		await page.goto("/login");
		await expect(page.getByRole("heading", { name: /Bienvenido de vuelta/ })).toBeVisible();

		await page.getByLabel("Email").fill("admin@passly.local");
		await page.getByLabel("Contraseña").fill("admin123");
		await page.getByRole("button", { name: /Iniciar sesión/ }).click();

		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });
		await expect(page.getByRole("heading", { name: /encuentra tu próximo/i })).toBeVisible();
	});

	test("logout: vuelve a estado anónimo", async ({ page }) => {
		await page.goto("/login");
		await page.getByLabel("Email").fill("admin@passly.local");
		await page.getByLabel("Contraseña").fill("admin123");
		await page.getByRole("button", { name: /Iniciar sesión/ }).click();
		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });

		await page.getByRole("button", { name: /Menú de usuario/ }).click();
		await page.getByText("Cerrar sesión").click();
		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByRole("link", { name: /Iniciar sesión/ })).toBeVisible();
	});

	test("registro con formulario: crea usuario y queda logueado", async ({ page }) => {
		const username = `user_${Date.now()}`;

		await page.goto("/register");
		await expect(page.getByRole("heading", { name: /Crear una cuenta/ })).toBeVisible();

		await page.getByRole("textbox", { name: "Nombre", exact: true }).fill("Usuario");
		await page.getByLabel("Apellido").fill("De Prueba");
		await page.getByLabel("Email").fill(`${username}@example.com`);
		await page.getByLabel("Nombre de usuario").fill(username);
		await page.getByLabel("Contraseña").fill("passly123");
		await page.getByRole("button", { name: /Crear cuenta/ }).click();

		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });
		await expect(page.getByRole("heading", { name: /encuentra tu próximo/i })).toBeVisible();
	});
});
