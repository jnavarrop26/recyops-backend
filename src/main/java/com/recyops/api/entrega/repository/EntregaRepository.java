package com.recyops.api.entrega.repository;

import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntregaRepository extends JpaRepository<Entrega, UUID> {

    @Query("""
            select e from Entrega e
            where (:bodegaId is null or e.bodega.id = :bodegaId)
              and (:proveedorId is null or e.proveedor.id = :proveedorId)
              and (:estado is null or e.estado = :estado)
              and (cast(:fechaDesde as timestamp) is null or e.fechaRecepcion >= :fechaDesde)
              and (cast(:fechaHasta as timestamp) is null or e.fechaRecepcion <= :fechaHasta)
            order by e.fechaRecepcion desc
            """)
    Page<Entrega> buscar(
            @Param("bodegaId") UUID bodegaId,
            @Param("proveedorId") UUID proveedorId,
            @Param("estado") EstadoEntrega estado,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable paginacion);

    List<Entrega> findByProveedorIdOrderByFechaRecepcionDesc(UUID proveedorId);

    long countByFechaRecepcionBetween(LocalDateTime desde, LocalDateTime hasta);

    @Query(value = "select nextval('entregas_codigo_seq')", nativeQuery = true)
    long siguienteConsecutivo();

    /** Agregado por dia para el tablero: [fecha, cantidad, peso recibido]. */
    @Query(value = """
            select cast(fecha_recepcion as date) as dia,
                   count(*) as cantidad,
                   coalesce(sum(peso_kg), 0) as peso
            from entregas
            where fecha_recepcion >= :desde
            group by cast(fecha_recepcion as date)
            """, nativeQuery = true)
    List<Object[]> resumenDiario(@Param("desde") LocalDateTime desde);
}
