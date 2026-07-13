package com.recyops.api.usuario.repository;

import com.recyops.api.usuario.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findBySupabaseId(UUID supabaseId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<Usuario> findByBodegaId(UUID bodegaId);
}
