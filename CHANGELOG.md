# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto usa [Versionamiento Semántico](https://semver.org/lang/es/).

Mientras la versión sea `0.x.y`, la API se considera en desarrollo inicial:
puede haber cambios incompatibles en un `MINOR` sin que eso implique un `MAJOR`.

## [Sin publicar]

## [0.1.0] - 2026-08-14

Primer release documentado del backend. Reúne el trabajo realizado hasta la fecha.

### Added

- Arquitectura multi-tenant por esquema de PostgreSQL, con `FiltroEsquemaEmpresa`,
  `ResolutorEsquemaEmpresa` y `ProveedorConexionesPorEsquema`.
- Autenticación vía JWT de Supabase (ES256), con extracción de rol
  (`ROLE_ADMIN` / `ROLE_OPERARIO`) desde `app_metadata`.
- Módulo de super admin para provisionamiento de nuevas empresas.
- Recibo en PDF de entregas y trazabilidad de pagos de ingresos.
- Paginación en los listados de tareas, trabajadores e ingresos.
- Documentación de la API en cliente Bruno.
- Suite de tests unitarios de la capa de servicio (179 tests) con cobertura vía JaCoCo.
- Suite de tests unitarios de controladores.
- Base de tests de integración con Testcontainers (Postgres real).
- Configuración de Dependabot para actualizaciones automáticas de dependencias.

### Changed

- Migración del sistema de logs de Logback a Log4j2, con logging asíncrono (LMAX Disruptor).
- Migración de la gestión de migraciones de esquema a Flyway (una corrida por tenant).
- Corrección de N+1 en listados.
- Alineación de paquetes de test con su ubicación real y activación de Failsafe
  para tests de integración.

### Fixed

- Manejo de errores 400 vs. 500 en controladores.
- ADR-002 — endurecimiento de seguridad y confiabilidad:
  - Puerto de gestión separado para Actuator.
  - Bloqueo optimista en `LineaInventario`.
  - Timeouts en los `RestClient` de Auth y Supabase Admin.
  - Validación de límites de `page`/`size` en controladores paginados.
  - `TransactionTemplate` en el provisionamiento de empresas.

### Security

- Corrección de vulnerabilidades de seguridad detectadas y adición de log
  transaccional dedicado (`transacciones.log`).

[Sin publicar]: https://github.com/jnavarrop26/recyops-backend/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/jnavarrop26/recyops-backend/releases/tag/v0.1.0
