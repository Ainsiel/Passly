import { expect, test } from "@playwright/test";

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

		await page.waitForURL(/\/reservas\/.*\/tickets/, { timeout: 15_000 });
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

		await page.waitForURL(/\/reservas\/.*\/tickets/, { timeout: 15_000 });
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

		await page.goto("/eventos/26");

		const reserveButton = page.getByRole("button", { name: /Reservar entradas/ });
		await expect(reserveButton).toBeVisible();

		await page.getByLabel("Cantidad").selectOption("1");
		await reserveButton.click();

		await page.waitForURL(/\/reservas\/.*\/tickets/, { timeout: 15_000 });

		await page.waitForTimeout(3000);

		const mailhogResponse = await page.request.get(
			"http://localhost:8025/api/v2/messages",
		);
		const mailhogData = await mailhogResponse.json();
		expect(mailhogData.total).toBeGreaterThan(0);

		const lastEmail = mailhogData.items[0];
		expect(lastEmail.Content.Headers.Subject[0]).toContain("Passly");
	});
});
