package com.recyops.api.tarea.interfaces;

import com.recyops.api.tarea.dtos.CuerpoAvance;
import com.recyops.api.tarea.dtos.CuerpoTarea;
import com.recyops.api.tarea.dtos.RespuestaAvance;
import com.recyops.api.tarea.dtos.RespuestaTarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import java.util.List;
import java.util.UUID;

public interface TareaService {

    /** Tablero del admin: todas las tareas con filtros opcionales. */
    List<RespuestaTarea> listar(EstadoTarea estado, UUID asignadoId, UUID bodegaId);

    /** Lista del operario autenticado, ordenada por urgencia. */
    List<RespuestaTarea> misTareas();

    RespuestaTarea crear(CuerpoTarea cuerpo);

    RespuestaTarea editar(UUID id, CuerpoTarea cuerpo);

    /**
     * El admin puede mover a cualquier estado; el operario asignado
     * solo puede avanzar su tarea al siguiente estado del flujo
     * (y nunca mas alla de COMPLETADA: Revision es del admin).
     */
    RespuestaTarea cambiarEstado(UUID id, EstadoTarea valor);

    /** Bitacora de la tarea; visible para el admin y para el asignado. */
    List<RespuestaAvance> listarAvances(UUID tareaId);

    /** El asignado registra avances mientras la tarea esta EN_PROGRESO; el admin siempre. */
    RespuestaAvance agregarAvance(UUID tareaId, CuerpoAvance cuerpo);

    /** Borrado definitivo (tarea + bitacora); solo admin y solo en REVISION. */
    void eliminar(UUID tareaId);
}
