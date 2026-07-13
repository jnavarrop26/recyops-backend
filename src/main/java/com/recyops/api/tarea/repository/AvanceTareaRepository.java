package com.recyops.api.tarea.repository;

import com.recyops.api.tarea.entity.AvanceTarea;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvanceTareaRepository extends JpaRepository<AvanceTarea, UUID> {

    List<AvanceTarea> findByTareaIdOrderByFechaRegistroAsc(UUID tareaId);
}
