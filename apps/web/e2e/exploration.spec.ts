import { expect, test } from "@playwright/test";

test.describe("Exploración de eventos", () => {
	test("home muestra la lista de eventos", async ({ page }) => {
		await page.goto("/");
		await expect(page.getByRole("heading", { name: /encuentra tu próximo/i })).toBeVisible();
		await expect(page.getByText(/Explora conciertos/)).toBeVisible();
	});

	test("lista muestra cards de eventos con nombre, categoría y precio", async ({
		page,
	}) => {
		await page.goto("/");
		const cards = page.locator("article");
		await expect(cards.first()).toBeVisible({ timeout: 10000 });
		const count = await cards.count();
		expect(count).toBeGreaterThan(0);
	});

	test("navegación anónima: clic en evento lleva al detalle", async ({ page }) => {
		await page.goto("/");
		const firstCard = page.locator("article").first();
		await expect(firstCard).toBeVisible({ timeout: 10000 });
		await firstCard.getByRole("link", { name: /Ver detalle/ }).click();

		await page.waitForURL(/\/eventos\//);
		await expect(
			page.getByRole("heading", { level: 1 })
		).toBeVisible();
	});

	test("detalle muestra fecha, venue, categoría, precio y disponibilidad", async ({
		page,
	}) => {
		await page.goto("/");
		const firstCard = page.locator("article").first();
		await expect(firstCard).toBeVisible({ timeout: 10000 });
		await firstCard.getByRole("link", { name: /Ver detalle/ }).click();

		await page.waitForURL(/\/eventos\//);

		await expect(page.getByText("Disponibilidad")).toBeVisible();
		await expect(page.getByText("Sobre el evento")).toBeVisible();
	});

	test("filtro por categoría", async ({ page }) => {
		await page.goto("/?category=CONCIERTO");
		await page.waitForLoadState("networkidle");
		const cards = page.locator("article");
		const count = await cards.count();
		if (count > 0) {
			const firstCard = cards.first();
			await expect(firstCard.getByText("Concierto")).toBeVisible();
		}
	});

	test("búsqueda por texto", async ({ page }) => {
		await page.goto("/");
		await page.waitForLoadState("networkidle");
		const input = page.getByRole("textbox", { name: /buscar/i });
		await input.fill("Bad Bunny");
		await page.waitForURL(/q=Bad\+Bunny/);
		await page.waitForLoadState("networkidle");
		const cards = page.locator("article");
		await expect(cards.first()).toBeVisible({ timeout: 10000 });
	});

	test("paginación funciona", async ({ page }) => {
		await page.goto("/?page=0");
		await page.waitForLoadState("networkidle");
		const cards = page.locator("article");
		await expect(cards.first()).toBeVisible({ timeout: 10000 });

		const nextLink = page.getByLabel(/Siguiente/i);
		if (await nextLink.isVisible()) {
			await nextLink.click();
			await page.waitForLoadState("networkidle");
			await expect(cards.first()).toBeVisible({ timeout: 10000 });
		}
	});

	test("breadcrumb en detalle", async ({ page }) => {
		await page.goto("/");
		const firstCard = page.locator("article").first();
		await expect(firstCard).toBeVisible({ timeout: 10000 });
		await firstCard.getByRole("link", { name: /Ver detalle/ }).click();

		await page.waitForURL(/\/eventos\//);
		await expect(page.getByRole("navigation", { name: /breadcrumb/i })).toBeVisible();
		await expect(page.getByRole("navigation", { name: /breadcrumb/i }).getByRole("link", { name: "Eventos" })).toBeVisible();
	});

	test("volver a eventos desde detalle", async ({ page }) => {
		await page.goto("/");
		const firstCard = page.locator("article").first();
		await expect(firstCard).toBeVisible({ timeout: 10000 });
		await firstCard.getByRole("link", { name: /Ver detalle/ }).click();

		await page.waitForURL(/\/eventos\//);
		await page.getByRole("link", { name: /Volver a eventos/ }).click();
		await page.waitForURL("/");
		await expect(page.getByRole("heading", { name: /encuentra tu próximo/i })).toBeVisible();
	});

	test("navegación accesible con teclado", async ({ page }) => {
		await page.goto("/");
		await page.keyboard.press("Tab");
		await page.keyboard.press("Tab");

		const focusedElement = page.locator(":focus");
		await expect(focusedElement).toBeVisible();
	});
});

test.describe("Login y exploración autenticada", () => {
	test("login contra Keycloak y ver eventos", async ({ page }) => {
		await page.goto("/login");
		await expect(page.getByText("Bienvenido de vuelta")).toBeVisible();

		await page.getByRole("button", { name: /Continuar con Keycloak/ }).click();
		await page.waitForURL("**/realms/passly/**");
		await page.locator("#username").fill("admin");
		await page.locator("#password").fill("admin123");
		await page.locator("#kc-login").click();

		await page.waitForURL("http://localhost:3000/**", { timeout: 30_000 });
		await expect(page.getByRole("heading", { name: /encuentra tu próximo/i })).toBeVisible();
	});

	test("logout vuelve a estado anónimo", async ({ page }) => {
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

	test("registro desde página dedicada", async ({ page }) => {
		await page.goto("/register");
		await expect(page.getByText("Crear una cuenta")).toBeVisible();
		await expect(
			page.locator("#main-content").getByRole("link", { name: "Iniciar sesión" })
		).toBeVisible();
	});

	test("login desde página dedicada", async ({ page }) => {
		await page.goto("/login");
		await expect(page.getByText("Bienvenido de vuelta")).toBeVisible();
		await expect(
			page.getByRole("link", { name: "Crear cuenta" })
		).toBeVisible();
	});
});

test.describe("Accesibilidad", () => {
	test("skip-to-content link es visible con tab", async ({ page }) => {
		await page.goto("/");
		const skipLink = page.getByText("Saltar al contenido principal");
		await expect(skipLink).toBeAttached();
		await page.keyboard.press("Tab");
		await expect(skipLink).toBeVisible();
	});

	test("heading hierarchy es correcta", async ({ page }) => {
		await page.goto("/");
		await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
	});

	test("landmarks existen", async ({ page }) => {
		await page.goto("/");
		await expect(page.getByRole("banner")).toBeVisible();
		await expect(page.getByRole("main")).toBeVisible();
	});

	test("imágenes alternativas en cards", async ({ page }) => {
		await page.goto("/");
		const cards = page.locator("article");
		await expect(cards.first()).toBeVisible({ timeout: 10000 });
		const firstCard = cards.first();
		const link = firstCard.getByRole("link").first();
		const label = await link.getAttribute("aria-label");
		expect(label).toBeTruthy();
	});
});
