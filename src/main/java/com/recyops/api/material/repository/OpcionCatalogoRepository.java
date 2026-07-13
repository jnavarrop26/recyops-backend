package com.recyops.api.material.repository;

import com.recyops.api.material.entity.OpcionCatalogo;
import com.recyops.api.material.enums.TipoOpcionCatalogo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpcionCatalogoRepository extends JpaRepository<OpcionCatalogo, UUID> {

    List<OpcionCatalogo> findByTipoOrderByNombre(TipoOpcionCatalogo tipo);

    List<OpcionCatalogo> findByTipoAndCategoriaPadreCodigoOrderByNombre(TipoOpcionCatalogo tipo,
            String categoriaPadreCodigo);

    Optional<OpcionCatalogo> findByTipoAndCodigo(TipoOpcionCatalogo tipo, String codigo);
}
