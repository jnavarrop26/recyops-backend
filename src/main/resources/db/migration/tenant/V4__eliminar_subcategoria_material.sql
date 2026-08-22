-- Elimina Subcategoria del catalogo de materiales: nunca se uso para
-- distinguir precio ni trazabilidad, solo agregaba un paso extra al alta.
alter table materiales drop column subcategoria_id;

delete from opciones_catalogo where tipo = 'SUBCATEGORIA';

alter table opciones_catalogo drop column categoria_padre_codigo;

alter table opciones_catalogo drop constraint opciones_catalogo_tipo_check;
alter table opciones_catalogo add constraint opciones_catalogo_tipo_check
    check (tipo in ('CATEGORIA', 'RESINA', 'COLOR'));
