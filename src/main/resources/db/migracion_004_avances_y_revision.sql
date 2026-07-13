-- ============================================================================
-- Migracion 004: bitacora de avances por tarea + estado REVISION en el flujo.
-- Ejecutar UNA vez por cada esquema de empresa ya creado.
-- ============================================================================

set search_path to empresa_demo;

-- Nuevo estado REVISION en el flujo de tareas
alter table tareas drop constraint if exists tareas_estado_check;
alter table tareas add constraint tareas_estado_check
    check (estado in ('PENDIENTE','EN_PROGRESO','COMPLETADA','REVISION','CANCELADA'));

-- Bitacora de avances (evidencia de trabajo, inmutable)
create table if not exists avances_tarea (
    id             uuid primary key default gen_random_uuid(),
    tarea_id       uuid not null references tareas (id) on delete cascade,
    cantidad       numeric(14,2),
    descripcion    varchar(300) not null,
    usuario_nombre varchar(200) not null,
    fecha_registro timestamp not null default now()
);

create index if not exists idx_avances_tarea on avances_tarea (tarea_id);

set search_path to public;
