package com.recyops.api.usuario.repository;

import com.recyops.api.usuario.entity.Rol;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, UUID> {
}
