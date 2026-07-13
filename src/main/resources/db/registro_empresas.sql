-- ============================================================================
-- RecyOps - Registro central de empresas (se ejecuta UNA sola vez en Supabase)
-- Vive en el esquema public y solo guarda que empresas existen y que esquema
-- de Postgres le corresponde a cada una. Los datos de negocio NUNCA van aqui.
-- ============================================================================

create table if not exists public.empresas (
    id             uuid primary key default gen_random_uuid(),
    nombre         varchar(200) not null,
    nit            varchar(30)  not null unique,
    -- Nombre del esquema Postgres asignado (ej: empresa_ecoverde).
    -- Debe cumplir el patron ^[a-z][a-z0-9_]{0,62}$
    schema_name    varchar(63)  not null unique,
    activo         boolean      not null default true,
    fecha_creacion timestamp    not null default now()
);

comment on table public.empresas is
    'Directorio de empresas RecyOps: mapea cada empresa a su esquema de datos aislado';
