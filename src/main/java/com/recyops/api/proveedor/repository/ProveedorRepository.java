package com.recyops.api.proveedor.repository;

import com.recyops.api.proveedor.entity.Proveedor;
import com.recyops.api.proveedor.enums.EstadoProveedor;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    @Query("""
            select p from Proveedor p
            where (:estado is null or p.estado = :estado)
              and (:nombre is null or lower(p.nombre) like lower(concat('%', cast(:nombre as string), '%')))
            order by p.nombre
            """)
    Page<Proveedor> buscar(
            @Param("estado") EstadoProveedor estado,
            @Param("nombre") String nombre,
            Pageable paginacion);
}
