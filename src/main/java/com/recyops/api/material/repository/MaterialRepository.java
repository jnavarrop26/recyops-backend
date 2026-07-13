package com.recyops.api.material.repository;

import com.recyops.api.material.entity.Material;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    @Query("""
            select m from Material m
            where (:categoria is null or m.categoria.codigo = :categoria)
              and (:resina is null or m.resina.codigo = :resina)
              and (:color is null or m.color.codigo = :color)
              and (:empaque is null or str(m.unidadEmpaque) = :empaque)
              and (:activo is null or m.activo = :activo)
            order by m.nombre
            """)
    Page<Material> buscar(
            @Param("categoria") String categoria,
            @Param("resina") String resina,
            @Param("color") String color,
            @Param("empaque") String empaque,
            @Param("activo") Boolean activo,
            Pageable paginacion);
}
