package com.recyops.api.tarea.service;

import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.UsuarioAutenticado;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.comun.log.LogTransaccional;
import com.recyops.api.tarea.dtos.CuerpoAvance;
import com.recyops.api.tarea.dtos.CuerpoTarea;
import com.recyops.api.tarea.dtos.RespuestaAvance;
import com.recyops.api.tarea.dtos.RespuestaTarea;
import com.recyops.api.tarea.entity.AvanceTarea;
import com.recyops.api.tarea.entity.Tarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import com.recyops.api.tarea.excepciones.TareaAjenaException;
import com.recyops.api.tarea.excepciones.TareaNoEncontradaException;
import com.recyops.api.tarea.excepciones.TransicionTareaInvalidaException;
import com.recyops.api.tarea.interfaces.TareaService;
import com.recyops.api.tarea.repository.AvanceTareaRepository;
import com.recyops.api.tarea.repository.TareaRepository;
import com.recyops.api.usuario.excepciones.UsuarioNoEncontradoException;
import com.recyops.api.usuario.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TareaServiceImpl implements TareaService {

    private final TareaRepository tareaRepository;
    private final AvanceTareaRepository avanceRepository;
    private final UsuarioRepository usuarioRepository;
    private final BodegaRepository bodegaRepository;

    public TareaServiceImpl(TareaRepository tareaRepository, AvanceTareaRepository avanceRepository,
            UsuarioRepository usuarioRepository, BodegaRepository bodegaRepository) {
        this.tareaRepository = tareaRepository;
        this.avanceRepository = avanceRepository;
        this.usuarioRepository = usuarioRepository;
        this.bodegaRepository = bodegaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaTarea> listar(EstadoTarea estado, UUID asignadoId, UUID bodegaId) {
        return tareaRepository.buscar(estado, asignadoId, bodegaId).stream()
                .map(RespuestaTarea::desde)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaTarea> misTareas() {
        UUID supabaseId = UsuarioAutenticado.supabaseId();
        if (supabaseId == null) {
            return List.of();
        }
        return usuarioRepository.findBySupabaseId(supabaseId)
                .map(usuario -> tareaRepository.findByAsignadoIdOrderByFechaCreacionDesc(usuario.getId()).stream()
                        .map(RespuestaTarea::desde)
                        .sorted(porUrgencia())
                        .toList())
                .orElse(List.of());
    }

    /** ALTA primero; a igual prioridad, la fecha limite mas proxima. */
    private Comparator<RespuestaTarea> porUrgencia() {
        return Comparator
                .comparing((RespuestaTarea t) -> t.prioridad().ordinal(), Comparator.reverseOrder())
                .thenComparing(t -> t.fechaLimite() != null ? t.fechaLimite() : LocalDate.MAX);
    }

    @Override
    public RespuestaTarea crear(CuerpoTarea cuerpo) {
        Tarea tarea = Tarea.builder()
                .creadoPorNombre(UsuarioAutenticado.nombreCompleto())
                .build();
        aplicarCuerpo(tarea, cuerpo);
        return RespuestaTarea.desde(tareaRepository.save(tarea));
    }

    @Override
    public RespuestaTarea editar(UUID id, CuerpoTarea cuerpo) {
        Tarea tarea = buscarEntidad(id);
        aplicarCuerpo(tarea, cuerpo);
        return RespuestaTarea.desde(tarea);
    }

    @Override
    @LogTransaccional(operacion = "TAREA_ESTADO_CAMBIADO")
    public RespuestaTarea cambiarEstado(UUID id, EstadoTarea valor) {
        Tarea tarea = buscarEntidad(id);

        if (!UsuarioAutenticado.esAdmin()) {
            // El operario solo mueve SUS tareas, solo hacia adelante,
            // y nunca mas alla de COMPLETADA (Revision la maneja el admin)
            validarAsignado(tarea);
            if (tarea.getEstado().siguiente() != valor || valor == EstadoTarea.REVISION) {
                throw new TransicionTareaInvalidaException(tarea.getEstado(), valor);
            }
        }

        tarea.setEstado(valor);
        tarea.setFechaCompletada(valor == EstadoTarea.COMPLETADA ? LocalDateTime.now() : null);
        return RespuestaTarea.desde(tarea);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaAvance> listarAvances(UUID tareaId) {
        Tarea tarea = buscarEntidad(tareaId);
        if (!UsuarioAutenticado.esAdmin()) {
            validarAsignado(tarea);
        }
        return avanceRepository.findByTareaIdOrderByFechaRegistroAsc(tareaId).stream()
                .map(RespuestaAvance::desde)
                .toList();
    }

    @Override
    public RespuestaAvance agregarAvance(UUID tareaId, CuerpoAvance cuerpo) {
        Tarea tarea = buscarEntidad(tareaId);
        if (!UsuarioAutenticado.esAdmin()) {
            validarAsignado(tarea);
            if (tarea.getEstado() != EstadoTarea.EN_PROGRESO) {
                throw new ReglaNegocioException(
                        "Solo puedes registrar avances mientras la tarea esta en progreso");
            }
        }
        AvanceTarea avance = AvanceTarea.builder()
                .tarea(tarea)
                .cantidad(cuerpo.cantidad())
                .descripcion(cuerpo.descripcion().trim())
                .usuarioNombre(UsuarioAutenticado.nombreCompleto())
                .build();
        return RespuestaAvance.desde(avanceRepository.save(avance));
    }

    @Override
    public void eliminar(UUID tareaId) {
        Tarea tarea = buscarEntidad(tareaId);
        // El borrado definitivo exige haber pasado por revision del admin
        if (tarea.getEstado() != EstadoTarea.REVISION) {
            throw new ReglaNegocioException(
                    "Solo se pueden eliminar tareas en Revision; muevela alla primero");
        }
        avanceRepository.deleteAll(avanceRepository.findByTareaIdOrderByFechaRegistroAsc(tareaId));
        tareaRepository.delete(tarea);
    }

    private void validarAsignado(Tarea tarea) {
        UUID supabaseId = UsuarioAutenticado.supabaseId();
        UUID asignadoSupabase = tarea.getAsignado().getSupabaseId();
        if (supabaseId == null || !supabaseId.equals(asignadoSupabase)) {
            throw new TareaAjenaException();
        }
    }

    private void aplicarCuerpo(Tarea tarea, CuerpoTarea cuerpo) {
        var asignado = usuarioRepository.findById(cuerpo.asignadoId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(cuerpo.asignadoId()));
        tarea.setTitulo(cuerpo.titulo());
        tarea.setDescripcion(cuerpo.descripcion());
        tarea.setAsignado(asignado);
        tarea.setBodega(cuerpo.bodegaId() == null ? null
                : bodegaRepository.findById(cuerpo.bodegaId())
                        .orElseThrow(() -> new BodegaNoEncontradaException(cuerpo.bodegaId())));
        tarea.setPrioridad(cuerpo.prioridad());
        tarea.setFechaLimite(cuerpo.fechaLimite());
    }

    private Tarea buscarEntidad(UUID id) {
        return tareaRepository.findById(id).orElseThrow(() -> new TareaNoEncontradaException(id));
    }
}
