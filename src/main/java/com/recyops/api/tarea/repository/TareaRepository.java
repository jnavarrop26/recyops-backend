package com.recyops.api.tarea.repository;

import com.recyops.api.tarea.entity.Tarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TareaRepository extends JpaRepository<Tarea, UUID> {

    @Query("""
            select t from Tarea t
            where (:estado is null or t.estado = :estado)
              and (:asignadoId is null or t.asignado.id = :asignadoId)
              and (:bodegaId is null or t.bodega.id = :bodegaId)
            order by t.fechaCreacion desc
            """)
    List<Tarea> buscar(
            @Param("estado") EstadoTarea estado,
            @Param("asignadoId") UUID asignadoId,
            @Param("bodegaId") UUID bodegaId);

    List<Tarea> findByAsignadoIdOrderByFechaCreacionDesc(UUID asignadoId);
}
