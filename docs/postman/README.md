# Postman - PowerZone API

Colecciones y entorno para probar la API de PowerZone (`/api/v1`).

## Archivos

- `PowerZone.API.postman_collection.json`: colección organizada por módulos (Auth, Users, Routines, Progress, Billing).
- `PowerZone.local.postman_environment.json`: entorno local con variables (`baseUrl`, `accessToken`, etc.).

## Uso

1. Importar en Postman la colección y el entorno.
2. Seleccionar el entorno **PowerZone Local** y ajustar `baseUrl` si es necesario (por defecto `http://localhost:3000`).
3. Habilitar el manejo de cookies en Postman (el refresh de sesión usa la cookie HttpOnly `pz_refresh` con `path=/api/v1/auth`).
4. Ejecutar `Auth > Login` con credenciales válidas:
   - el script de **Tests** guarda `accessToken`, `userId` y `gymId` en el entorno.
5. Las requests protegidas usan `Authorization: Bearer {{accessToken}}` (configurado a nivel de colección). Las requests públicas están marcadas como `No Auth`.

## Notas

- `POST /api/v1/auth/register` crea el usuario (rol por defecto `MEMBER`); para obtener sesión usar `login`.
- `POST /api/v1/auth/refresh` renueva el access token usando la cookie `pz_refresh` (no requiere body).
- `PUT /api/v1/users/{userId}/role` requiere rol `ADMIN`. `GET /api/v1/users` requiere `ADMIN` o `TRAINER`.
- `PUT /api/v1/routines/{routineId}/exercises/{routineExerciseId}` requiere `ADMIN` o `TRAINER`.
- `POST /api/v1/billing/webhook` es público pero exige firma HMAC (`X-PowerZone-Signature`, `X-PowerZone-Timestamp`) y no usa JWT.
- Para descargar comprobantes PDF usar **Send and Download** en `Billing > Receipt (PDF)`.
- También se puede generar/actualizar una base de colección importando OpenAPI desde `{{baseUrl}}/api/docs-json`.

## Roles y autorización (referencia)

- Roles definidos: `MEMBER`, `TRAINER`, `ADMIN`, `SUPER_ADMIN`.
- El JWT transporta el claim `roles`; el backend lo convierte a autoridades `ROLE_*`.
- La autorización se aplica por reglas de ruta (`SecurityConfig`), anotaciones (`@PreAuthorize`) y validaciones de negocio en servicios/controladores según el endpoint.
