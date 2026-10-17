# Convencion de commits

PowerZone usa [Conventional Commits](https://www.conventionalcommits.org/) en
espanol para que el historial sea legible y permita generar notas de release.

## Formato

```
<tipo>(<alcance>): <descripcion> [PPP-000]

<cuerpo opcional>

Refs: PPP-000
```

- **tipo**: obligatorio, en minusculas.
- **alcance**: opcional, modulo afectado (`api`, `db`, `web`, `security`, `docs`).
- **descripcion**: imperativo, minusculas, sin punto final, maximo 72 caracteres.
- **Referencia Jira**: en el asunto entre corchetes y en el pie como `Refs:`.

## Tipos permitidos

| Tipo | Uso |
|---|---|
| `feat` | Funcionalidad nueva visible para el usuario |
| `fix` | Correccion de un defecto |
| `refactor` | Reestructuracion sin cambio de comportamiento |
| `test` | Pruebas nuevas o ajustadas |
| `docs` | Documentacion |
| `build` | Dependencias, Docker, empaquetado |
| `ci` | Pipeline y automatizacion |
| `chore` | Mantenimiento sin impacto funcional |

## Ejemplos

```
feat(auth): implementar registro de miembros [PPP-17]

Crea el endpoint POST /api/v1/auth/register con validacion de datos,
hash BCrypt y asignacion del rol MEMBER al gimnasio activo.

Refs: PPP-17
```

```
fix(web): evitar doble envio en el formulario de acceso [PPP-289]

Refs: PPP-289
```

```
ci: ejecutar pruebas de frontend en el pipeline [PPP-277]

Refs: PPP-277
```

## Reglas del equipo

1. Un commit resuelve una intencion; si el mensaje necesita "y" suele sobrar.
2. No se commitean credenciales, tokens ni archivos `.env`.
3. Las migraciones Flyway van en su propio commit y no se editan una vez aplicadas.
4. El cuerpo explica **que** y **por que**, no repite el diff.
5. Todo commit referencia al menos una tarea Jira mediante `Refs:`.

## Trazabilidad

| Elemento | Identificador |
|---|---|
| Historia / tarea | `PPP-000` |
| Rama | `feat/PPP-000-descripcion` |
| Commit | `tipo(alcance): descripcion [PPP-000]` + `Refs: PPP-000` |
| Pull request | Titulo Conventional Commit y seccion "Tareas Jira" |
| Release | `release/sprint-N` + tag `sprint-N` |
