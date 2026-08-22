-- Elimina el sistema de calificacion (estrellas) de proveedores: no alimentaba
-- ninguna decision de negocio y quedaba desactualizado manualmente.
alter table proveedores drop column calificacion;
