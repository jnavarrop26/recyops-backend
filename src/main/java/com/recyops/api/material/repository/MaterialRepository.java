package com.recyops.api.material.repository;

import com.recyops.api.material.entity.Material;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    /**
     * Las cuatro opciones de catalogo vienen resueltas ({@code RespuestaMaterial}
     * lee codigo y nombre de todas). Los joins son explicitos y con alias a
     * proposito: filtrar por {@code m.resina.codigo} obligaba a Hibernate a un
     * inner join implicito, que dejaba fuera del listado a los materiales sin
     * resina, subcategoria o color aunque no se estuviera filtrando por ellos.
     */
    @Query(value = """
            select m from Material m
            join fetch m.categoria cat
            left join fetch m.subcategoria
            left join fetch m.resina res
            left join fetch m.color col
            where (:categoria is null or cat.codigo = :categoria)
              and (:resina is null or res.codigo = :resina)
              and (:color is null or col.codigo = :color)
              and (:empaque is null or str(m.unidadEmpaque) = :empaque)
              and (:activo is null or m.activo = :activo)
            order by m.nombre
            """,
            countQuery = """
            select count(m) from Material m
            join m.categoria cat
            left join m.resina res
            left join m.color col
            where (:categoria is null or cat.codigo = :categoria)
              and (:resina is null or res.codigo = :resina)
              and (:color is null or col.codigo = :color)
              and (:empaque is null or str(m.unidadEmpaque) = :empaque)
              and (:activo is null or m.activo = :activo)
            """)
    Page<Material> buscar(
            @Param("categoria") String categoria,
            @Param("resina") String resina,
            @Param("color") String color,
            @Param("empaque") String empaque,
            @Param("activo") Boolean activo,
            Pageable paginacion);
}
