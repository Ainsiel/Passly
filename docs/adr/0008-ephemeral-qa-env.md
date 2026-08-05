# Entorno QA efímero en GitHub Actions

No hay despliegue en producción. El entorno QA es efímero: GitHub Actions levanta docker compose en el runner, corre smoke y tests de carga (k6), publica los reportes como artifact y destruye todo. Se descartó desplegar en cloud free-tier o VPS porque este proyecto es de portfolio y no necesita disponibilidad permanente; el QA efímero demuestra CI/CD real sin costo ni superficie de mantenimiento.
