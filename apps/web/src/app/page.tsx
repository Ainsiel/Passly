import { auth } from "@/auth";
import { login, logout, register } from "@/app/actions";
import { fetchMe } from "@/lib/catalog";

export default async function HomePage() {
	const session = await auth();
	const me = await fetchMe(session?.accessToken);

	return (
		<main>
			<h1>Passly</h1>
			<p>Validación de sesión en Server Components contra Keycloak.</p>

			{session?.user ? (
				<form action={logout}>
					<p>
						Sesión iniciada como <strong>{session.user.username ?? session.user.name}</strong>.
					</p>
					<p>Roles del JWT: {session.user.roles?.join(", ") || "ninguno"}</p>
					<button type="submit">Cerrar sesión</button>
				</form>
			) : (
				<form action={login}>
					<button type="submit">Iniciar sesión con Keycloak</button>
					<button type="submit" formAction={register}>
						Registrarse
					</button>
				</form>
			)}

			<section
				className={me.ok ? "status ok" : "status unauthorized"}
				data-testid="me-status"
			>
				<h2>GET /api/catalog/me vía gateway</h2>
				<p>
					HTTP <strong data-testid="me-status-code">{me.status}</strong>
				</p>
				{me.ok ? (
					<p data-testid="me-body">
						<code>{JSON.stringify(me.body)}</code>
					</p>
				) : (
					<p data-testid="me-body">
						Sin token válido: el resource-server rechazó la petición.
					</p>
				)}
			</section>
		</main>
	);
}
