package com.recyops.api.entrega.repository;

import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntregaRepository extends JpaRepository<Entrega, UUID> {

    /**
     * El listado no trae las lineas (ver {@code RespuestaEntrega.desde}), asi que
     * solo hace falta el join a convenio/bodega/persona; total_kg ya viene
     * denormalizado en la fila.
     */
    @Query(value = """
            select e from Entrega e
            left join fetch e.convenio
            join fetch e.bodega
            left join fetch e.personaEntrega
            where (:bodegaId is null or e.bodega.id = :bodegaId)
              and (:convenioId is null or e.convenio.id = :convenioId)
              and (:estado is null or e.estado = :estado)
              and (cast(:fechaDesde as timestamp) is null or e.fechaRecepcion >= :fechaDesde)
              and (cast(:fechaHasta as timestamp) is null or e.fechaRecepcion <= :fechaHasta)
            order by e.fechaRecepcion desc
            """,
            countQuery = """
            select count(e) from Entrega e
            where (:bodegaId is null or e.bodega.id = :bodegaId)
              and (:convenioId is null or e.convenio.id = :convenioId)
              and (:estado is null or e.estado = :estado)
              and (cast(:fechaDesde as timestamp) is null or e.fechaRecepcion >= :fechaDesde)
              and (cast(:fechaHasta as timestamp) is null or e.fechaRecepcion <= :fechaHasta)
            """)
    Page<Entrega> buscar(
            @Param("bodegaId") UUID bodegaId,
            @Param("convenioId") UUID convenioId,
            @Param("estado") EstadoEntrega estado,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable paginacion);

    /** Entregas de los convenios de un proveedor (ver RespuestaEntregaProveedor). */
    @Query("""
            select e from Entrega e
            where e.convenio.proveedor.id = :proveedorId
            order by e.fechaRecepcion desc
            """)
    List<Entrega> findByConvenioProveedorIdOrderByFechaRecepcionDesc(@Param("proveedorId") UUID proveedorId);

    /** Entrega con convenio, bodega, persona y lineas+material (recibo PDF). */
    @Query("""
            select e from Entrega e
            left join fetch e.convenio
            join fetch e.bodega
            left join fetch e.personaEntrega
            left join fetch e.lineas l
            left join fetch l.tipoMaterial
            where e.id = :id
            """)
    Optional<Entrega> buscarConRelaciones(@Param("id") UUID id);

    long countByFechaRecepcionBetween(LocalDateTime desde, LocalDateTime hasta);

    @Query(value = "select nextval('entregas_codigo_seq')", nativeQuery = true)
    long siguienteConsecutivo();

    /** Agregado por dia para el tablero: [fecha, cantidad, peso recibido]. */
    @Query(value = """
            select cast(fecha_recepcion as date) as dia,
                   count(*) as cantidad,
                   coalesce(sum(total_kg), 0) as peso
            from entregas
            where fecha_recepcion >= :desde
            group by cast(fecha_recepcion as date)
            """, nativeQuery = true)
    List<Object[]> resumenDiario(@Param("desde") LocalDateTime desde);
}
