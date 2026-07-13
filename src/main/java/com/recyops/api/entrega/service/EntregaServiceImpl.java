package com.recyops.api.entrega.service;

import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.UsuarioAutenticado;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.entrega.dtos.CuerpoEntrega;
import com.recyops.api.entrega.dtos.RespuestaEntrega;
import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import com.recyops.api.entrega.excepciones.EntregaNoEncontradaException;
import com.recyops.api.entrega.excepciones.TransicionEstadoInvalidaException;
import com.recyops.api.entrega.interfaces.EntregaService;
import com.recyops.api.entrega.repository.EntregaRepository;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.inventario.interfaces.InventarioService;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.repository.MaterialRepository;
import com.recyops.api.proveedor.excepciones.ProveedorNoEncontradoException;
import com.recyops.api.proveedor.repository.ProveedorRepository;
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
    private final ProveedorRepository proveedorRepository;
    private final BodegaRepository bodegaRepository;
    private final MaterialRepository materialRepository;
    private final InventarioService inventarioService;

    public EntregaServiceImpl(EntregaRepository entregaRepository, ProveedorRepository proveedorRepository,
            BodegaRepository bodegaRepository, MaterialRepository materialRepository,
            InventarioService inventarioService) {
        this.entregaRepository = entregaRepository;
        this.proveedorRepository = proveedorRepository;
        this.bodegaRepository = bodegaRepository;
        this.materialRepository = materialRepository;
        this.inventarioService = inventarioService;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaEntrega> listar(UUID bodegaId, UUID proveedorId, EstadoEntrega estado,
            LocalDate fechaDesde, LocalDate fechaHasta, int page, int size) {
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
        var pagina = entregaRepository.buscar(bodegaId, proveedorId, estado, desde, hasta,
                PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaEntrega::desde);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEntrega obtener(UUID id) {
        return RespuestaEntrega.desde(buscarEntidad(id));
    }

    @Override
    public RespuestaEntrega registrar(CuerpoEntrega cuerpo) {
        var proveedor = proveedorRepository.findById(cuerpo.proveedorId())
                .orElseThrow(() -> new ProveedorNoEncontradoException(cuerpo.proveedorId()));
        var bodega = bodegaRepository.findById(cuerpo.bodegaId())
                .orElseThrow(() -> new BodegaNoEncontradaException(cuerpo.bodegaId()));
        var material = materialRepository.findById(cuerpo.tipoMaterialId())
                .orElseThrow(() -> new MaterialNoEncontradoException(cuerpo.tipoMaterialId()));

        Entrega entrega = Entrega.builder()
                .codigo("ENT-%06d".formatted(entregaRepository.siguienteConsecutivo()))
                .proveedor(proveedor)
                .bodega(bodega)
                .tipoMaterial(material)
                .pesoKg(cuerpo.pesoKg())
                .personaEntrega(cuerpo.personaEntrega())
                .fechaRecepcion(cuerpo.fechaRecepcion() != null ? cuerpo.fechaRecepcion() : LocalDateTime.now())
                .usuarioRegistroNombre(UsuarioAutenticado.nombreCompleto())
                .build();
        entrega = entregaRepository.save(entrega);

        // El material recibido entra al inventario de la bodega desde ya
        inventarioService.registrarEntrada(bodega.getId(), material.getId(),
                entrega.getPesoKg(), entrega.getCodigo());

        return RespuestaEntrega.desde(entrega);
    }

    @Override
    public RespuestaEntrega cambiarEstado(UUID id, EstadoEntrega valor) {
        Entrega entrega = buscarEntidad(id);
        // Solo se permite avanzar al siguiente estado del flujo de trazabilidad.
        if (entrega.getEstado().siguiente() != valor) {
            throw new TransicionEstadoInvalidaException(entrega.getEstado(), valor);
        }
        // Al despachar, el material sale del inventario de la bodega
        if (valor == EstadoEntrega.DESPACHADA) {
            inventarioService.registrarSalida(entrega.getBodega().getId(),
                    entrega.getTipoMaterial().getId(), entrega.getPesoKg(),
                    "Despacho " + entrega.getCodigo());
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
        // Revierte la entrada que registro la recepcion, dejando rastro auditable
        inventarioService.registrarSalida(entrega.getBodega().getId(),
                entrega.getTipoMaterial().getId(), entrega.getPesoKg(),
                "Eliminacion " + entrega.getCodigo());
        entregaRepository.delete(entrega);
    }

    private Entrega buscarEntidad(UUID id) {
        return entregaRepository.findById(id).orElseThrow(() -> new EntregaNoEncontradaException(id));
    }
}
