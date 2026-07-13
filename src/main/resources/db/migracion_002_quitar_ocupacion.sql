-- ============================================================================
-- Migracion 002: se elimina la ocupacion/capacidad de bodegas (obsoleta).
-- El stock real ahora vive en lineas_inventario, alimentado por las entregas.
-- Ejecutar UNA vez por cada esquema de empresa ya creado.
-- ============================================================================

set search_path to empresa_demo;

alter table bodegas drop column if exists capacidad_maxima;
alter table bodegas drop column if exists ocupacion_actual;

set search_path to public;
