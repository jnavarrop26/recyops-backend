package com.recyops.api.usuario.repository;

import com.recyops.api.usuario.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findBySupabaseId(UUID supabaseId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Listado de trabajadores con rol y bodega resueltos: {@code RespuestaTrabajador}
     * lee el nombre de ambos, y un {@code findAll()} pelado costaba dos selects
     * extra por usuario. La bodega es opcional, por eso {@code left join}.
     */
    @Query("""
            select u from Usuario u
            join fetch u.rol
            left join fetch u.bodega
            order by u.nombreCompleto
            """)
    List<Usuario> listarConRolYBodega();

    /** Con el rol resuelto: {@code RespuestaUsuarioBodega} lee su nombre. */
    @Query("""
            select u from Usuario u
            join fetch u.rol
            where u.bodega.id = :bodegaId
            """)
    List<Usuario> findByBodegaId(@Param("bodegaId") UUID bodegaId);
}
