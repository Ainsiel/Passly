# Keycloak como proveedor de identidad

Keycloak es el proveedor de identidad. El frontend usa Authorization Code + PKCE vía Auth.js v5 (provider Keycloak); cada servicio valida el JWT como resource-server (`spring-boot-starter-oauth2-resource-server`); el gateway propaga la autenticación. Se descartó emitir JWT desde el propio backend porque delegar la identidad a un IdP maduro es lo que un sistema real hace y demuestra integración OIDC completa. Keycloak corre en docker compose con el realm versionado en `realm-export.json` (import con `--import-realm`).
