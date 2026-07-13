package com.recyops.api.convenio.repository;

import com.recyops.api.convenio.entity.Convenio;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConvenioRepository extends JpaRepository<Convenio, UUID> {

    @Query("""
            select c from Convenio c
            where (:estado is null or c.estado = :estado)
              and (:tipo is null or c.tipo = :tipo)
              and (:nombre is null or lower(c.nombre) like lower(concat('%', cast(:nombre as string), '%')))
            order by c.fechaCreacion desc
            """)
    Page<Convenio> buscar(
            @Param("estado") EstadoConvenio estado,
            @Param("tipo") TipoConvenio tipo,
            @Param("nombre") String nombre,
            Pageable paginacion);

    @Query(value = "select nextval('convenios_codigo_seq')", nativeQuery = true)
    long siguienteConsecutivo();
}
