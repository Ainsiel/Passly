import { expect, test } from "@playwright/test";

test.describe("Auth de punta a punta (ticket #3)", () => {
	test("usuario anónimo: no ve opciones de sesión cerrada", async ({ page }) => {
		await page.goto("/");
		await expect(page.getByRole("button", { name: /Menú de usuario/ })).toBeHidden();
		await expect(page.getByRole("link", { name: /Iniciar sesión/ })).toBeVisible();
	});

	test("login con Keycloak: queda logueado en home", async ({ page }) => {
		await page.goto("/login");
		await page.getByRole("button", { name: /Continuar con Keycloak/ }).click();

		await page.waitForURL("**/realms/passly/**");
		await page.locator("#username").fill("admin");
		await page.locator("#password").fill("admin123");
		await page.locator("#kc-login").click();

		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByRole("heading", { name: /encuentra tu próximo/i })).toBeVisible();
	});

	test("logout: vuelve a estado anónimo", async ({ page }) => {
		await page.goto("/login");
		await page.getByRole("button", { name: /Continuar con Keycloak/ }).click();
		await page.waitForURL("**/realms/passly/**");
		await page.locator("#username").fill("admin");
		await page.locator("#password").fill("admin123");
		await page.locator("#kc-login").click();
		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });

		// Open user dropdown menu
		await page.getByRole("button", { name: /Menú de usuario/ }).click();
		await page.getByText("Cerrar sesión").click();
		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByRole("link", { name: /Iniciar sesión/ })).toBeVisible();
	});

	test("registro con Keycloak: crea usuario y queda logueado", async ({ page }) => {
		const username = `user_${Date.now()}`;

		await page.goto("/register");
		await page.getByRole("button", { name: /Continuar con Keycloak/ }).click();

		await page.waitForURL("**/realms/passly/**", { timeout: 30_000 });
		await expect(
			page.getByRole("heading", { name: "Register" })
		).toBeVisible();
		await page.locator("#firstName").fill("Usuario");
		await page.locator("#lastName").fill("De Prueba");
		await page.locator("#email").fill(`${username}@example.com`);
		await page.locator("#username").fill(username);
		await page.locator("#password").fill("passly123");
		await page.locator("#password-confirm").fill("passly123");
		await page.getByRole("button", { name: "Register" }).click();

		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByRole("heading", { name: /encuentra tu próximo/i })).toBeVisible();
	});
});
