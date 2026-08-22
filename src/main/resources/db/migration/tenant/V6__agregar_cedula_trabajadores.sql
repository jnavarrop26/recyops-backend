-- Cedula del trabajador: la necesita el formulario de Entrega para
-- autocompletarla al elegir quien entrega. Nullable: los trabajadores
-- historicos no la tienen todavia.
alter table usuarios add column cedula varchar(30) unique;
