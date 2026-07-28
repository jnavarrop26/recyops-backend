# Migraciones historicas (pre-Flyway)

Estos `.psql` **ya no se ejecutan**. Se conservan como registro de como evoluciono
el esquema antes de adoptar Flyway (2026-07-28).

Todo su efecto acumulado esta consolidado en
`db/migration/tenant/V1__esquema_base.sql`, que es la linea base del historial.
Los esquemas que ya existian cuando se adopto Flyway se *baselinearon* en V1 en
lugar de re-ejecutar nada.

## Por que se dejaron de usar

Se aplicaban a mano con `psql -v esquema=empresa_x -f <archivo>.psql`, uno por
esquema de empresa, y en paralelo habia que copiar el mismo cambio a las
plantillas `esquema_empresa.psql` / `esquema_empresa_demo.psql` — porque el
provisionamiento por API solo ejecutaba la plantilla, nunca las migraciones.

Ese doble mantenimiento fallo al menos una vez: la columna
`avances_tarea.revisado` llego a la base de `empresa_demo` sin pasar por ningun
archivo de este directorio ni por las plantillas. Ver el ADR en
`docs/adr/001-flyway-migraciones-multi-tenant.md`.

## Migraciones nuevas

Van en `db/migration/tenant/` como `V2__...sql`, `V3__...sql`, etc. SQL plano:
sin metacomandos psql (`\if`, `\set`), sin `create schema` y sin `search_path` —
Flyway fija el esquema por conexion.
