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

    @Query("""
            select l from LineaInventario l
            where l.bodega.id = :bodegaId
              and (:tipoMaterialId is null or l.tipoMaterial.id = :tipoMaterialId)
              and (:bajoMinimo is null or :bajoMinimo = false or l.stockActual < l.stockMinimo)
            order by l.tipoMaterial.nombre
            """)
    Page<LineaInventario> buscar(
            @Param("bodegaId") UUID bodegaId,
            @Param("tipoMaterialId") UUID tipoMaterialId,
            @Param("bajoMinimo") Boolean bajoMinimo,
            Pageable paginacion);

    @Query("select count(l) from LineaInventario l where l.stockActual < l.stockMinimo")
    long contarBajoMinimo();
}
