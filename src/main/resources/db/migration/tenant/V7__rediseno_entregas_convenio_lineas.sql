-- Rediseno de Entrega: de "1 entrega = 1 proveedor + 1 material + 1 peso" a
-- un encabezado con N lineas de material, ligado a un Convenio en vez de a
-- un Proveedor directo, con la persona que entrega referenciando un
-- trabajador real en vez de texto libre.

create table detalle_entregas (
    id               uuid primary key default gen_random_uuid(),
    entrega_id       uuid not null references entregas (id) on delete cascade,
    tipo_material_id uuid not null references materiales (id),
    peso_kg          numeric(14,2) not null
);

create index idx_detalle_entregas_entrega  on detalle_entregas (entrega_id);
create index idx_detalle_entregas_material on detalle_entregas (tipo_material_id);

-- Preserva las entregas existentes como una linea cada una.
insert into detalle_entregas (entrega_id, tipo_material_id, peso_kg)
select id, tipo_material_id, peso_kg from entregas;

alter table entregas add column convenio_id uuid references convenios (id);
alter table entregas add column persona_entrega_id uuid references usuarios (id);
alter table entregas add column total_kg numeric(14,2);

update entregas e
set total_kg = coalesce((select sum(d.peso_kg) from detalle_entregas d where d.entrega_id = e.id), 0);

alter table entregas alter column total_kg set not null;

alter table entregas drop column tipo_material_id;
alter table entregas drop column peso_kg;
alter table entregas drop column proveedor_id;
alter table entregas drop column persona_entrega;

create index idx_entregas_convenio_fecha on entregas (convenio_id, fecha_recepcion desc);
