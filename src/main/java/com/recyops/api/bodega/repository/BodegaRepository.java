package com.recyops.api.bodega.repository;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.enums.TipoOrganizacion;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BodegaRepository extends JpaRepository<Bodega, UUID> {

    @Query("""
            select b from Bodega b
            where (:estado is null or b.estado = :estado)
              and (:tipoOrganizacion is null or b.tipoOrganizacion = :tipoOrganizacion)
              and (:nombre is null or lower(b.nombre) like lower(concat('%', cast(:nombre as string), '%')))
            order by b.nombre
            """)
    Page<Bodega> buscar(
            @Param("estado") EstadoBodega estado,
            @Param("tipoOrganizacion") TipoOrganizacion tipoOrganizacion,
            @Param("nombre") String nombre,
            Pageable paginacion);

    long countByEstado(EstadoBodega estado);
}
