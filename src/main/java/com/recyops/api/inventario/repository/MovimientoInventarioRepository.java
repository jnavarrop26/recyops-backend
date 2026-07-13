package com.recyops.api.inventario.repository;

import com.recyops.api.inventario.entity.MovimientoInventario;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, UUID> {

    Page<MovimientoInventario> findByLineaIdOrderByFechaRegistroDesc(UUID lineaId, Pageable paginacion);
}
