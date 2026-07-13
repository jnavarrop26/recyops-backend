package com.recyops.api.ingreso.repository;

import com.recyops.api.ingreso.entity.IngresoMaterial;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngresoMaterialRepository extends JpaRepository<IngresoMaterial, Long> {

    Optional<IngresoMaterial> findByUuid(UUID uuid);

    @Query("""
            select i from IngresoMaterial i
            where (cast(:fechaDesde as timestamp) is null or i.fecha >= :fechaDesde)
              and (cast(:fechaHasta as timestamp) is null or i.fecha <= :fechaHasta)
            order by i.fecha desc
            """)
    List<IngresoMaterial> buscarPorRango(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta);

    /** Agregado por dia para el tablero: [fecha, cantidad, peso total, valor total]. */
    @Query(value = """
            select cast(fecha as date) as dia,
                   count(*) as cantidad,
                   coalesce(sum(peso_neto_total), 0) as peso,
                   coalesce(sum(total), 0) as valor
            from ingresos_material
            where fecha >= :desde
            group by cast(fecha as date)
            """, nativeQuery = true)
    List<Object[]> resumenDiario(@Param("desde") LocalDateTime desde);
}
