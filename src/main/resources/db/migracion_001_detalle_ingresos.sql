-- ============================================================================
-- Migracion 001: detalle de materiales por ingreso + UUID del recibo
-- Ejecutar UNA vez por cada esquema de empresa ya creado.
-- (Para esquemas nuevos no hace falta: esquema_empresa.sql ya lo incluye.)
-- ============================================================================

set search_path to empresa_demo;

alter table ingresos_material
    add column if not exists uuid uuid not null unique default gen_random_uuid();

create table if not exists detalles_ingreso (
    id            uuid primary key default gen_random_uuid(),
    ingreso_id    bigint not null references ingresos_material (id) on delete cascade,
    categoria     varchar(100) not null,
    peso_bruto    numeric(14,2) not null,
    tara          numeric(14,2) not null default 0,
    peso_neto     numeric(14,2) not null,
    precio_kilo   numeric(14,2) not null,
    subtotal      numeric(16,2) not null,
    observaciones varchar(500)
);

create index if not exists idx_detalles_ingreso on detalles_ingreso (ingreso_id);

set search_path to public;
