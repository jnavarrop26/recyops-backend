-- ============================================================================
-- V2 - Bloqueo optimista en lineas_inventario
--
-- Las operaciones de entrada/salida/ajuste/merma hacen read-modify-write sobre
-- stock_actual sin ningun control de concurrencia. Esta columna la usa
-- Hibernate (@Version) para detectar si dos transacciones pisaron la misma
-- fila: incrementa en cada UPDATE y falla si el valor leido ya no coincide.
-- ============================================================================

alter table lineas_inventario add column version bigint not null default 0;
