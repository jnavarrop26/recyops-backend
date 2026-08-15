alter table ingresos_material alter column bodega_destino drop not null;
alter table ingresos_material add column bodega_destino_id uuid references bodegas(id);
