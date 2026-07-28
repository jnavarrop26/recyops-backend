package com.recyops.api.inventario.repository;

import com.recyops.api.inventario.entity.LineaInventario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LineaInventarioRepository extends JpaRepository<LineaInventario, UUID> {

    boolean existsByBodegaIdAndTipoMaterialId(UUID bodegaId, UUID tipoMaterialId);

    Optional<LineaInventario> findByBodegaIdAndTipoMaterialId(UUID bodegaId, UUID tipoMaterialId);

    /**
     * Bodega, material y la categoria del material en una sola consulta:
     * {@code RespuestaLineaInventario} baja hasta {@code tipoMaterial.categoria.nombre},
     * asi que sin el fetch cada linea de la pagina costaba tres selects extra.
     */
    @Query(value = """
            select l from LineaInventario l
            join fetch l.bodega
            join fetch l.tipoMaterial m
            join fetch m.categoria
            where l.bodega.id = :bodegaId
              and (:tipoMaterialId is null or l.tipoMaterial.id = :tipoMaterialId)
              and (:bajoMinimo is null or :bajoMinimo = false or l.stockActual < l.stockMinimo)
            order by m.nombre
            """,
            countQuery = """
            select count(l) from LineaInventario l
            where l.bodega.id = :bodegaId
              and (:tipoMaterialId is null or l.tipoMaterial.id = :tipoMaterialId)
              and (:bajoMinimo is null or :bajoMinimo = false or l.stockActual < l.stockMinimo)
            """)
    Page<LineaInventario> buscar(
            @Param("bodegaId") UUID bodegaId,
            @Param("tipoMaterialId") UUID tipoMaterialId,
            @Param("bajoMinimo") Boolean bajoMinimo,
            Pageable paginacion);

    @Query("select count(l) from LineaInventario l where l.stockActual < l.stockMinimo")
    long contarBajoMinimo();
}
