package com.recyops.api.ingreso.service;

import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.ingreso.dtos.CuerpoDetalleIngreso;
import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.CuerpoPago;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import com.recyops.api.ingreso.enums.EstadoIngreso;
import com.recyops.api.ingreso.enums.EstadoPago;
import com.recyops.api.ingreso.entity.DetalleIngreso;
import com.recyops.api.ingreso.entity.IngresoMaterial;
import com.recyops.api.comun.log.LogTransaccional;
import com.recyops.api.ingreso.excepciones.IngresoNoEncontradoException;
import com.recyops.api.ingreso.interfaces.IngresoService;
import com.recyops.api.ingreso.repository.IngresoMaterialRepository;
import com.recyops.api.material.entity.Material;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.repository.MaterialRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IngresoServiceImpl implements IngresoService {

    private final IngresoMaterialRepository ingresoRepository;
    private final MaterialRepository materialRepository;

    public IngresoServiceImpl(IngresoMaterialRepository ingresoRepository,
            MaterialRepository materialRepository) {
        this.ingresoRepository = ingresoRepository;
        this.materialRepository = materialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaIngreso> historial(LocalDate fechaDesde, LocalDate fechaHasta) {
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
        return ingresoRepository.buscarPorRango(desde, hasta).stream()
                .map(RespuestaIngreso::desde)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaIngreso obtener(Long id) {
        return ingresoRepository.findById(id)
                .map(RespuestaIngreso::conDetalles)
                .orElseThrow(() -> new IngresoNoEncontradoException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaIngreso obtenerPorUuid(UUID uuid) {
        return ingresoRepository.findByUuid(uuid)
                .map(RespuestaIngreso::conDetalles)
                .orElseThrow(() -> new IngresoNoEncontradoException(uuid));
    }

    @Override
    @LogTransaccional(operacion = "INGRESO_REGISTRADO")
    public RespuestaIngreso registrar(CuerpoIngreso cuerpo) {
        IngresoMaterial ingreso = IngresoMaterial.builder()
                .cliente(cuerpo.cliente())
                .cedula(cuerpo.cedula())
                .bodegaDestino(cuerpo.bodegaDestino())
                .encargado(cuerpo.encargado())
                .placaVehiculo(cuerpo.placaVehiculo())
                .pesoNetoTotal(cuerpo.pesoNetoTotal())
                .total(cuerpo.total())
                .build();

        // Con detalle de materiales, los totales autoritativos se calculan aqui.
        if (cuerpo.materiales() != null && !cuerpo.materiales().isEmpty()) {
            BigDecimal pesoTotal = BigDecimal.ZERO;
            BigDecimal valorTotal = BigDecimal.ZERO;
            for (CuerpoDetalleIngreso material : cuerpo.materiales()) {
                DetalleIngreso detalle = construirDetalle(ingreso, material);
                ingreso.getDetalles().add(detalle);
                pesoTotal = pesoTotal.add(detalle.getPesoNeto());
                valorTotal = valorTotal.add(detalle.getSubtotal());
            }
            ingreso.setPesoNetoTotal(pesoTotal);
            ingreso.setTotal(valorTotal);
        }

        return RespuestaIngreso.conDetalles(ingresoRepository.save(ingreso));
    }

    @Override
    @LogTransaccional(operacion = "INGRESO_PAGADO")
    public RespuestaIngreso registrarPago(Long id, CuerpoPago cuerpo) {
        IngresoMaterial ingreso = buscarEntidad(id);
        // Si ya esta pagado, solo se permite corregir el metodo por uno distinto.
        if (ingreso.getEstadoPago() == EstadoPago.PAGADO
                && ingreso.getMetodoPago() == cuerpo.metodoPago()) {
            throw new ReglaNegocioException(
                    "El ingreso " + id + " ya fue pagado con " + ingreso.getMetodoPago());
        }
        ingreso.setEstadoPago(EstadoPago.PAGADO);
        ingreso.setMetodoPago(cuerpo.metodoPago());
        return RespuestaIngreso.conDetalles(ingreso);
    }

    @Override
    @LogTransaccional(operacion = "INGRESO_ESTADO_CAMBIADO")
    public RespuestaIngreso cambiarEstado(Long id, EstadoIngreso valor) {
        IngresoMaterial ingreso = buscarEntidad(id);
        ingreso.setEstado(valor);
        return RespuestaIngreso.conDetalles(ingreso);
    }

    @Override
    @LogTransaccional(operacion = "INGRESO_PASO_CAMBIADO")
    public RespuestaIngreso cambiarPaso(Long id, boolean valor) {
        IngresoMaterial ingreso = buscarEntidad(id);
        ingreso.setPaso(valor);
        return RespuestaIngreso.conDetalles(ingreso);
    }

    private IngresoMaterial buscarEntidad(Long id) {
        return ingresoRepository.findById(id)
                .orElseThrow(() -> new IngresoNoEncontradoException(id));
    }

    private DetalleIngreso construirDetalle(IngresoMaterial ingreso, CuerpoDetalleIngreso cuerpo) {
        // Con materialId, la categoria y el precio salen del catalogo de la empresa;
        // sin el, se conserva el flujo viejo de texto libre y precio digitado.
        Material material = cuerpo.materialId() != null
                ? materialRepository.findById(cuerpo.materialId())
                        .orElseThrow(() -> new MaterialNoEncontradoException(cuerpo.materialId()))
                : null;

        String categoria = material != null ? material.getNombre() : cuerpo.categoria();
        if (categoria == null || categoria.isBlank()) {
            throw new ReglaNegocioException("Cada material del ingreso requiere materialId o categoria");
        }
        BigDecimal precioKilo = cuerpo.precioKilo() != null
                ? cuerpo.precioKilo()
                : material != null ? material.getPrecioBase() : null;
        if (precioKilo == null) {
            throw new ReglaNegocioException(
                    "El material '" + categoria + "' requiere precio por kilo (explicito o del catalogo)");
        }

        BigDecimal pesoNeto = cuerpo.pesoBruto().subtract(cuerpo.tara()).max(BigDecimal.ZERO);
        return DetalleIngreso.builder()
                .ingreso(ingreso)
                .material(material)
                .categoria(categoria)
                .pesoBruto(cuerpo.pesoBruto())
                .tara(cuerpo.tara())
                .pesoNeto(pesoNeto)
                .precioKilo(precioKilo)
                .subtotal(pesoNeto.multiply(precioKilo))
                .observaciones(cuerpo.observaciones())
                .build();
    }
}
