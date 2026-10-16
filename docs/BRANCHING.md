# Politica de ramas y pull requests

Este documento define como se integra el trabajo en PowerZone. Aplica a los tres
integrantes del equipo y a cualquier colaborador futuro.

## Ramas permanentes

| Rama | Proposito | Quien integra |
|---|---|---|
| `main` | Codigo desplegable. Solo recibe releases cerrados. | Responsable del repositorio |
| `develop` | Integracion continua del sprint en curso. | Responsable del repositorio |

`main` y `develop` nunca reciben commits directos: todo entra por pull request,
incluso los cambios de una linea. La rama por defecto del repositorio es `main`.

## Ramas de trabajo

Se crean desde `develop` y se eliminan despues del merge.

| Prefijo | Uso | Ejemplo |
|---|---|---|
| `feat/PPP-000-descripcion` | Funcionalidad nueva | `feat/PPP-17-user-registration` |
| `fix/PPP-000-descripcion` | Correccion de defecto | `fix/PPP-291-refresh-loop` |
| `refactor/PPP-000-descripcion` | Cambio interno | `refactor/PPP-274-routine-service` |
| `test/PPP-000-descripcion` | Suites de prueba | `test/PPP-287-291-auth-tests` |
| `ci/PPP-000-descripcion` | Pipeline e infraestructura | `ci/PPP-74-pipeline` |
| `docs/PPP-000-descripcion` | Documentacion | `docs/PPP-304-branching` |
| `chore/PPP-000-descripcion` | Mantenimiento | `chore/PPP-1-bootstrap` |

Reglas:

- Una rama atiende **una** funcionalidad o tarea Jira; no se mezclan dominios.
- El nombre usa minusculas, guiones y el identificador Jira (`PPP-000`).
- La rama no dura mas de un sprint; si queda abierta se replantea.
- Antes de abrir el PR se hace `git rebase develop` o merge desde `develop`
  para resolver conflictos en la rama, nunca en el PR.

## Flujo de un pull request

1. Crear la rama desde `develop` actualizado.
2. Implementar con commits atomicos en Conventional Commits.
3. Ejecutar las verificaciones locales: `mvn -B verify` y `npm run build`.
4. Abrir el PR hacia `develop` con la plantilla completa.
5. Revisar el pipeline de CI: los tres jobs deben estar en verde.
6. Solicitar revision a otro integrante del equipo.
7. Aprobar y mergear con `--no-ff` para conservar el historial de la rama.
8. Eliminar la rama remota.

El PR debe declarar las tareas Jira y describir como probarlo. Un PR sin
criterios de prueba no se aprueba.

## Cierre de sprint

1. Crear la rama `release/sprint-N` desde `develop`.
2. Actualizar `docs/SPRINT-MAPPING.md` con el estado real.
3. Abrir PR de `release/sprint-N` hacia `main`.
4. Mergear y crear el tag anotado `sprint-N`.
5. Sincronizar `develop` con `main` para no arrastrar divergencia.

El sprint se representa con un **milestone** de GitHub, el PR de release y el
tag. No se usan ramas de larga duracion por sprint.

## Hotfix

Un defecto en produccion se corrige con `fix/PPP-000-descripcion` creada desde
`main`. El PR va a `main`, se etiqueta como `patch` y luego se porta a `develop`
para que el siguiente release no lo pierda.

## Protecciones recomendadas en GitHub

| Regla | `main` | `develop` |
|---|---|---|
| Requerir pull request | Si | Si |
| Requerir CI en verde | Si | Si |
| Requerir una aprobacion | Si | Si |
| Bloquear force push | Si | Si |
| Bloquear borrado | Si | Si |
| Requerir rama actualizada | Si | No |

## Responsabilidades

| Integrante | Rol | Alcance |
|---|---|---|
| Alejandro Alzate | Lider tecnico | Arquitectura, seguridad, merges finales y releases |
| Juan Jose Guarumo | Backend | API, persistencia, migraciones y pruebas |
| Juan Diego | Frontend | Angular, design system y pruebas de interfaz |

Los merges los ejecuta el responsable del repositorio despues de la aprobacion,
para que la trazabilidad de GitHub sea consistente.
