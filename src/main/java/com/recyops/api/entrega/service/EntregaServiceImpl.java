package com.recyops.api.entrega.service;

import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.UsuarioAutenticado;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.comun.log.LogTransaccional;
import com.recyops.api.convenio.excepciones.ConvenioNoEncontradoException;
import com.recyops.api.convenio.repository.ConvenioRepository;
import com.recyops.api.entrega.dtos.CuerpoEntrega;
import com.recyops.api.entrega.dtos.CuerpoLineaEntrega;
import com.recyops.api.entrega.dtos.RespuestaEntrega;
import com.recyops.api.entrega.dtos.RespuestaRecibo;
import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.entity.LineaEntrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import com.recyops.api.entrega.excepciones.EntregaNoEncontradaException;
import com.recyops.api.entrega.excepciones.TransicionEstadoInvalidaException;
import com.recyops.api.entrega.interfaces.EntregaService;
import com.recyops.api.entrega.repository.EntregaRepository;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.inventario.interfaces.InventarioService;
import com.recyops.api.material.entity.Material;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.repository.MaterialRepository;
import com.recyops.api.usuario.excepciones.UsuarioNoEncontradoException;
import com.recyops.api.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository entregaRepository;
    private final ConvenioRepository convenioRepository;
    private final BodegaRepository bodegaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final InventarioService inventarioService;
    private final GeneradorReciboPdf generadorReciboPdf;

    public EntregaServiceImpl(EntregaRepository entregaRepository, ConvenioRepository convenioRepository,
            BodegaRepository bodegaRepository, UsuarioRepository usuarioRepository,
            MaterialRepository materialRepository, InventarioService inventarioService,
            GeneradorReciboPdf generadorReciboPdf) {
        this.entregaRepository = entregaRepository;
        this.convenioRepository = convenioRepository;
        this.bodegaRepository = bodegaRepository;
        this.usuarioRepository = usuarioRepository;
        this.materialRepository = materialRepository;
        this.inventarioService = inventarioService;
        this.generadorReciboPdf = generadorReciboPdf;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaEntrega> listar(UUID bodegaId, UUID convenioId, EstadoEntrega estado,
            LocalDate fechaDesde, LocalDate fechaHasta, int page, int size) {
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
        var pagina = entregaRepository.buscar(bodegaId, convenioId, estado, desde, hasta,
                PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaEntrega::desde);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEntrega obtener(UUID id) {
        return RespuestaEntrega.conLineas(buscarEntidad(id));
    }

    @Override
    @LogTransaccional(operacion = "ENTREGA_REGISTRADA")
    public RespuestaEntrega registrar(CuerpoEntrega cuerpo) {
        var convenio = convenioRepository.findById(cuerpo.convenioId())
                .orElseThrow(() -> new ConvenioNoEncontradoException(cuerpo.convenioId()));
        var bodega = bodegaRepository.findById(cuerpo.bodegaId())
                .orElseThrow(() -> new BodegaNoEncontradaException(cuerpo.bodegaId()));
        var personaEntrega = usuarioRepository.findById(cuerpo.personaEntregaId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(cuerpo.personaEntregaId()));

        Entrega entrega = Entrega.builder()
                .codigo("ENT-%06d".formatted(entregaRepository.siguienteConsecutivo()))
                .convenio(convenio)
                .bodega(bodega)
                .personaEntrega(personaEntrega)
                .totalKg(BigDecimal.ZERO)
                .fechaRecepcion(cuerpo.fechaRecepcion() != null ? cuerpo.fechaRecepcion() : LocalDateTime.now())
                .usuarioRegistroNombre(UsuarioAutenticado.nombreCompleto())
                .build();

        BigDecimal totalKg = BigDecimal.ZERO;
        for (CuerpoLineaEntrega lineaCuerpo : cuerpo.lineas()) {
            LineaEntrega linea = construirLinea(entrega, lineaCuerpo);
            entrega.getLineas().add(linea);
            totalKg = totalKg.add(linea.getPesoKg());
        }
        entrega.setTotalKg(totalKg);

        Entrega guardada = entregaRepository.save(entrega);

        // El material recibido entra al inventario de la bodega desde ya, una
        // entrada de inventario por cada linea de material.
        for (LineaEntrega linea : guardada.getLineas()) {
            inventarioService.registrarEntrada(bodega.getId(), linea.getTipoMaterial().getId(),
                    linea.getPesoKg(), guardada.getCodigo());
        }

        return RespuestaEntrega.conLineas(guardada);
    }

    @Override
    public RespuestaEntrega cambiarEstado(UUID id, EstadoEntrega valor) {
        Entrega entrega = buscarEntidad(id);
        // Solo se permite avanzar al siguiente estado del flujo de trazabilidad.
        if (entrega.getEstado().siguiente() != valor) {
            throw new TransicionEstadoInvalidaException(entrega.getEstado(), valor);
        }
        // Al despachar, el material sale del inventario de la bodega, una salida
        // por cada linea de material.
        if (valor == EstadoEntrega.DESPACHADA) {
            for (LineaEntrega linea : entrega.getLineas()) {
                inventarioService.registrarSalida(entrega.getBodega().getId(),
                        linea.getTipoMaterial().getId(), linea.getPesoKg(),
                        "Despacho " + entrega.getCodigo());
            }
        }
        entrega.setEstado(valor);
        return RespuestaEntrega.desde(entrega);
    }

    @Override
    public void eliminar(UUID id) {
        Entrega entrega = buscarEntidad(id);
        if (entrega.getEstado() == EstadoEntrega.DESPACHADA) {
            throw new ReglaNegocioException(
                    "La entrega " + entrega.getCodigo() + " ya fue despachada y no se puede eliminar");
        }
        // Revierte las entradas que registro la recepcion, dejando rastro auditable.
        for (LineaEntrega linea : entrega.getLineas()) {
            inventarioService.registrarSalida(entrega.getBodega().getId(),
                    linea.getTipoMaterial().getId(), linea.getPesoKg(),
                    "Eliminacion " + entrega.getCodigo());
        }
        entregaRepository.delete(entrega);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaRecibo generarRecibo(UUID id) {
        // Con fetch join: el recibo usa convenio, bodega, persona y lineas en una sola consulta.
        Entrega entrega = entregaRepository.buscarConRelaciones(id)
                .orElseThrow(() -> new EntregaNoEncontradaException(id));
        return new RespuestaRecibo(entrega.getCodigo(), generadorReciboPdf.generar(entrega));
    }

    private LineaEntrega construirLinea(Entrega entrega, CuerpoLineaEntrega cuerpo) {
        Material material = materialRepository.findById(cuerpo.tipoMaterialId())
                .orElseThrow(() -> new MaterialNoEncontradoException(cuerpo.tipoMaterialId()));
        return LineaEntrega.builder()
                .entrega(entrega)
                .tipoMaterial(material)
                .pesoKg(cuerpo.pesoKg())
                .build();
    }

    private Entrega buscarEntidad(UUID id) {
        return entregaRepository.findById(id).orElseThrow(() -> new EntregaNoEncontradaException(id));
    }
}
