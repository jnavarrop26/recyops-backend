package com.recyops.api.dashboard.service;

import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.dashboard.dtos.RespuestaActividadDia;
import com.recyops.api.dashboard.dtos.RespuestaResumenDashboard;
import com.recyops.api.dashboard.interfaces.DashboardService;
import com.recyops.api.entrega.repository.EntregaRepository;
import com.recyops.api.ingreso.repository.IngresoMaterialRepository;
import com.recyops.api.inventario.repository.LineaInventarioRepository;
import com.recyops.api.proveedor.repository.ProveedorRepository;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final BodegaRepository bodegaRepository;
    private final ProveedorRepository proveedorRepository;
    private final EntregaRepository entregaRepository;
    private final LineaInventarioRepository lineaInventarioRepository;
    private final IngresoMaterialRepository ingresoRepository;

    public DashboardServiceImpl(BodegaRepository bodegaRepository,
            ProveedorRepository proveedorRepository,
            EntregaRepository entregaRepository,
            LineaInventarioRepository lineaInventarioRepository,
            IngresoMaterialRepository ingresoRepository) {
        this.bodegaRepository = bodegaRepository;
        this.proveedorRepository = proveedorRepository;
        this.entregaRepository = entregaRepository;
        this.lineaInventarioRepository = lineaInventarioRepository;
        this.ingresoRepository = ingresoRepository;
    }

    @Override
    public RespuestaResumenDashboard resumen() {
        LocalDate hoy = LocalDate.now();
        return new RespuestaResumenDashboard(
                bodegaRepository.count(),
                bodegaRepository.countByEstado(EstadoBodega.ACTIVA),
                proveedorRepository.count(),
                entregaRepository.countByFechaRecepcionBetween(hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX)),
                lineaInventarioRepository.contarBajoMinimo());
    }

    @Override
    public List<RespuestaActividadDia> actividadDiaria(int dias) {
        LocalDate hoy = LocalDate.now();
        var desde = hoy.minusDays(dias - 1L).atStartOfDay();

        // [fecha, cantidad, peso, valor] de ingresos y [fecha, cantidad, peso] de entregas
        Map<LocalDate, Object[]> ingresosPorDia = indexarPorFecha(ingresoRepository.resumenDiario(desde));
        Map<LocalDate, Object[]> entregasPorDia = indexarPorFecha(entregaRepository.resumenDiario(desde));

        List<RespuestaActividadDia> actividad = new ArrayList<>(dias);
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = hoy.minusDays(i);
            Object[] ingreso = ingresosPorDia.get(fecha);
            Object[] entrega = entregasPorDia.get(fecha);
            actividad.add(new RespuestaActividadDia(
                    fecha,
                    ingreso != null ? ((Number) ingreso[1]).longValue() : 0,
                    ingreso != null ? decimal(ingreso[2]) : BigDecimal.ZERO,
                    ingreso != null ? decimal(ingreso[3]) : BigDecimal.ZERO,
                    entrega != null ? ((Number) entrega[1]).longValue() : 0,
                    entrega != null ? decimal(entrega[2]) : BigDecimal.ZERO));
        }
        return actividad;
    }

    private Map<LocalDate, Object[]> indexarPorFecha(List<Object[]> filas) {
        Map<LocalDate, Object[]> indice = new HashMap<>();
        for (Object[] fila : filas) {
            indice.put(comoFecha(fila[0]), fila);
        }
        return indice;
    }

    private LocalDate comoFecha(Object valor) {
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor instanceof Date fechaSql) {
            return fechaSql.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(valor));
    }

    private BigDecimal decimal(Object valor) {
        return valor instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(valor));
    }
}
