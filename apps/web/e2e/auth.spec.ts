import { expect, test } from "@playwright/test";

test.describe("Auth de punta a punta (ticket #3)", () => {
	test("usuario anónimo: /api/catalog/me devuelve 401", async ({ page }) => {
		await page.goto("/");
		await expect(page.getByTestId("me-status-code")).toHaveText("401");
		await expect(page.getByTestId("me-body")).toContainText(
			"Sin token válido"
		);
		await expect(
			page.getByRole("button", { name: "Iniciar sesión con Keycloak" })
		).toBeVisible();
	});

	test("login con Keycloak: 401 -> 200 con usuario admin", async ({ page }) => {
		await page.goto("/");
		await page.getByRole("button", { name: "Iniciar sesión con Keycloak" }).click();

		await page.waitForURL("**/realms/passly/**");
		await page.locator("#username").fill("admin");
		await page.locator("#password").fill("admin123");
		await page.locator("#kc-login").click();

		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByTestId("me-status-code")).toHaveText("200");
		await expect(page.getByTestId("me-body")).toContainText("admin");
		await expect(
			page.getByRole("button", { name: "Cerrar sesión" })
		).toBeVisible();
	});

	test("logout: vuelve a 401", async ({ page }) => {
		await page.goto("/");
		await page.getByRole("button", { name: "Iniciar sesión con Keycloak" }).click();
		await page.waitForURL("**/realms/passly/**");
		await page.locator("#username").fill("admin");
		await page.locator("#password").fill("admin123");
		await page.locator("#kc-login").click();
		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByTestId("me-status-code")).toHaveText("200");

		await page.getByRole("button", { name: "Cerrar sesión" }).click();
		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByTestId("me-status-code")).toHaveText("401");
	});

	test("registro con Keycloak: crea usuario y queda logueado", async ({ page }) => {
		const username = `user_${Date.now()}`;

		await page.goto("/");
		await page.getByRole("button", { name: "Registrarse" }).click();

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
		await expect(page.getByTestId("me-status-code")).toHaveText("200");
		await expect(page.getByTestId("me-body")).toContainText(username);
		await expect(
			page.getByRole("button", { name: "Cerrar sesión" })
		).toBeVisible();
	});
});
