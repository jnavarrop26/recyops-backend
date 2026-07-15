package com.recyops.api.ingreso.service;

import com.recyops.api.ingreso.dtos.CuerpoDetalleIngreso;
import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import com.recyops.api.ingreso.entity.DetalleIngreso;
import com.recyops.api.ingreso.entity.IngresoMaterial;
import com.recyops.api.comun.log.LogTransaccional;
import com.recyops.api.ingreso.excepciones.IngresoNoEncontradoException;
import com.recyops.api.ingreso.interfaces.IngresoService;
import com.recyops.api.ingreso.repository.IngresoMaterialRepository;
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

    public IngresoServiceImpl(IngresoMaterialRepository ingresoRepository) {
        this.ingresoRepository = ingresoRepository;
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

    private DetalleIngreso construirDetalle(IngresoMaterial ingreso, CuerpoDetalleIngreso material) {
        BigDecimal pesoNeto = material.pesoBruto().subtract(material.tara()).max(BigDecimal.ZERO);
        return DetalleIngreso.builder()
                .ingreso(ingreso)
                .categoria(material.categoria())
                .pesoBruto(material.pesoBruto())
                .tara(material.tara())
                .pesoNeto(pesoNeto)
                .precioKilo(material.precioKilo())
                .subtotal(pesoNeto.multiply(material.precioKilo()))
                .observaciones(material.observaciones())
                .build();
    }
}
