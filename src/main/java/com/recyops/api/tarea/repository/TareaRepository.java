package com.recyops.api.tarea.repository;

import com.recyops.api.tarea.entity.Tarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TareaRepository extends JpaRepository<Tarea, UUID> {

    /** Asignado y bodega resueltos: {@code RespuestaTarea} lee el nombre de ambos. */
    @Query("""
            select t from Tarea t
            join fetch t.asignado
            left join fetch t.bodega
            where (:estado is null or t.estado = :estado)
              and (:asignadoId is null or t.asignado.id = :asignadoId)
              and (:bodegaId is null or t.bodega.id = :bodegaId)
            order by t.fechaCreacion desc
            """)
    List<Tarea> buscar(
            @Param("estado") EstadoTarea estado,
            @Param("asignadoId") UUID asignadoId,
            @Param("bodegaId") UUID bodegaId);

    /** Mismo fetch que {@link #buscar}: alimenta "mis tareas" del operario. */
    @Query("""
            select t from Tarea t
            join fetch t.asignado
            left join fetch t.bodega
            where t.asignado.id = :asignadoId
            order by t.fechaCreacion desc
            """)
    List<Tarea> findByAsignadoIdOrderByFechaCreacionDesc(@Param("asignadoId") UUID asignadoId);
}
