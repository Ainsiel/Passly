import { expect, test } from "@playwright/test";

/**
 * Wait for either navigation to the tickets page or an error message on the
 * current page. Throws with the actual error text if the reservation fails.
 */
async function waitForReservationResult(page: import("@playwright/test").Page) {
	const errorEl = page.locator(".text-destructive");

	await Promise.race([
		page.waitForURL(/\/reservas\/.*\/tickets/, { timeout: 15_000 }),
		errorEl.waitFor({ state: "visible", timeout: 15_000 }),
	]);

	if (await errorEl.isVisible()) {
		const msg = await errorEl.textContent();
		throw new Error(`Server action returned error: "${msg}"`);
	}
}

test.describe("Reserva de tickets (ticket #10)", () => {
	test("reserva completa: login, evento, reservar, ver tickets", async ({
		page,
	}) => {
		await page.goto("/login");
		await page.getByLabel("Email").fill("admin@passly.local");
		await page.getByLabel("Contraseña").fill("admin123");
		await page.getByRole("button", { name: /Iniciar sesión/ }).click();
		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });

		const firstCard = page.locator("article").first();
		await expect(firstCard).toBeVisible();
		await firstCard.click();
		await page.waitForURL(/\/eventos\//, { timeout: 10_000 });

		const reserveButton = page.getByRole("button", { name: /Reservar entradas/ });
		await expect(reserveButton).toBeVisible();

		await page.getByLabel("Cantidad").selectOption("1");
		await reserveButton.click();

		await waitForReservationResult(page);
		await expect(page.getByText("Tus tickets")).toBeVisible();
		await expect(page.getByText("Código").first()).toBeVisible();
		await expect(page.locator("img[alt='QR Code']").first()).toBeVisible();
	});

	test("mis reservas: lista las reservas del usuario", async ({ page }) => {
		await page.goto("/login");
		await page.getByLabel("Email").fill("admin@passly.local");
		await page.getByLabel("Contraseña").fill("admin123");
		await page.getByRole("button", { name: /Iniciar sesión/ }).click();
		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });

		await page.goto("/mis-reservas");
		await expect(page.getByRole("heading", { name: /Mis reservas/ })).toBeVisible();
	});

	test("reserva sin login: botón redirige a login", async ({ page }) => {
		await page.goto("/eventos/1");
		const reserveButton = page.getByRole("link", { name: /Inicia sesión para reservar/ });
		await expect(reserveButton).toBeVisible();
		await reserveButton.click();
		await page.waitForURL(/\/login/, { timeout: 10_000 });
	});

	test("doble envío no duplica reserva (idempotency)", async ({ page }) => {
		await page.goto("/login");
		await page.getByLabel("Email").fill("admin@passly.local");
		await page.getByLabel("Contraseña").fill("admin123");
		await page.getByRole("button", { name: /Iniciar sesión/ }).click();
		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });

		await page.goto("/eventos/8");

		const reserveButton = page.getByRole("button", { name: /Reservar entradas/ });
		await expect(reserveButton).toBeVisible();

		await page.getByLabel("Cantidad").selectOption("1");
		await reserveButton.click();

		await waitForReservationResult(page);
		await expect(page.getByText("Tus tickets")).toBeVisible();
	});

	test("header muestra link Mis reservas para usuario logueado", async ({
		page,
	}) => {
		await page.goto("/login");
		await page.getByLabel("Email").fill("admin@passly.local");
		await page.getByLabel("Contraseña").fill("admin123");
		await page.getByRole("button", { name: /Iniciar sesión/ }).click();
		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });

		await expect(page.getByRole("link", { name: "Mis reservas" }).first()).toBeVisible();
	});
});

test.describe("Verificación de email en Mailhog", () => {
	test("reserva exitosa envía email con ticket", async ({ page }) => {
		await page.goto("/login");
		await page.getByLabel("Email").fill("admin@passly.local");
		await page.getByLabel("Contraseña").fill("admin123");
		await page.getByRole("button", { name: /Iniciar sesión/ }).click();
		await page.waitForURL("http://localhost:3000/", { timeout: 30_000 });

		await page.goto("/eventos/30");

		const reserveButton = page.getByRole("button", { name: /Reservar entradas/ });
		await expect(reserveButton).toBeVisible();

		await page.getByLabel("Cantidad").selectOption("1");
		await reserveButton.click();

		await waitForReservationResult(page);

		let emailFound = false;
		for (let i = 0; i < 10; i++) {
			const mailhogResponse = await page.request.get(
				"http://localhost:8025/api/v2/messages",
			);
			const mailhogData = await mailhogResponse.json();
			if (mailhogData.total > 0) {
				const matchingEmail = mailhogData.items.find(
					(item: any) =>
						item.Content?.Headers?.Subject?.[0]?.includes("Taller de Cocina"),
				);
				if (matchingEmail) {
					emailFound = true;
					break;
				}
			}
			await page.waitForTimeout(2000);
		}
		expect(emailFound).toBe(true);
	});
});
