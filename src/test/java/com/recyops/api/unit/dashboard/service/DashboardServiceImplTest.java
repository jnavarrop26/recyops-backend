package com.recyops.api.unit.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.dashboard.service.DashboardServiceImpl;
import com.recyops.api.entrega.repository.EntregaRepository;
import com.recyops.api.ingreso.repository.IngresoMaterialRepository;
import com.recyops.api.inventario.repository.LineaInventarioRepository;
import com.recyops.api.proveedor.repository.ProveedorRepository;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private BodegaRepository bodegaRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private LineaInventarioRepository lineaInventarioRepository;

    @Mock
    private IngresoMaterialRepository ingresoRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // ---- resumen ----

    @Test
    void resumen_datosDisponibles_retornaIndicadoresDelTablero() {
        // Given
        when(bodegaRepository.count()).thenReturn(10L);
        when(bodegaRepository.countByEstado(EstadoBodega.ACTIVA)).thenReturn(7L);
        when(proveedorRepository.count()).thenReturn(15L);
        when(entregaRepository.countByFechaRecepcionBetween(any(), any())).thenReturn(3L);
        when(lineaInventarioRepository.contarBajoMinimo()).thenReturn(2L);

        // When
        var actualResumen = dashboardService.resumen();

        // Then
        assertThat(actualResumen.totalBodegas()).isEqualTo(10L);
        assertThat(actualResumen.bodegasActivas()).isEqualTo(7L);
        assertThat(actualResumen.totalProveedores()).isEqualTo(15L);
        assertThat(actualResumen.entregasHoy()).isEqualTo(3L);
        assertThat(actualResumen.lineasBajoMinimo()).isEqualTo(2L);
    }

    // ---- actividadDiaria ----

    @Test
    void actividadDiaria_diaConMovimiento_incluyeCantidadPesoYValor() {
        // Given
        LocalDate hoy = LocalDate.now();
        Object[] filaIngreso = { hoy, 3L, new BigDecimal("120.50"), new BigDecimal("500.00") };
        Object[] filaEntrega = { hoy, 2L, new BigDecimal("80.25") };
        when(ingresoRepository.resumenDiario(any())).thenReturn(List.<Object[]>of(filaIngreso));
        when(entregaRepository.resumenDiario(any())).thenReturn(List.<Object[]>of(filaEntrega));

        // When
        var actualActividad = dashboardService.actividadDiaria(1);

        // Then
        assertThat(actualActividad).hasSize(1);
        var actualDia = actualActividad.get(0);
        assertThat(actualDia.fecha()).isEqualTo(hoy);
        assertThat(actualDia.ingresos()).isEqualTo(3L);
        assertThat(actualDia.pesoIngresadoKg()).isEqualByComparingTo("120.50");
        assertThat(actualDia.valorIngresos()).isEqualByComparingTo("500.00");
        assertThat(actualDia.entregas()).isEqualTo(2L);
        assertThat(actualDia.pesoRecibidoKg()).isEqualByComparingTo("80.25");
    }

    @Test
    void actividadDiaria_diaSinMovimiento_rellenaConCeros() {
        // Given
        when(ingresoRepository.resumenDiario(any())).thenReturn(List.of());
        when(entregaRepository.resumenDiario(any())).thenReturn(List.of());

        // When
        var actualActividad = dashboardService.actividadDiaria(1);

        // Then
        assertThat(actualActividad).hasSize(1);
        var actualDia = actualActividad.get(0);
        assertThat(actualDia.fecha()).isEqualTo(LocalDate.now());
        assertThat(actualDia.ingresos()).isZero();
        assertThat(actualDia.pesoIngresadoKg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(actualDia.valorIngresos()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(actualDia.entregas()).isZero();
        assertThat(actualDia.pesoRecibidoKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void actividadDiaria_fechaDevueltaComoSqlDate_seIndexaEnElDiaCorrecto() {
        // Given
        LocalDate hoy = LocalDate.now();
        Object[] filaIngreso = { Date.valueOf(hoy), 1L, new BigDecimal("10.00"), new BigDecimal("20.00") };
        when(ingresoRepository.resumenDiario(any())).thenReturn(List.<Object[]>of(filaIngreso));
        when(entregaRepository.resumenDiario(any())).thenReturn(List.of());

        // When
        var actualActividad = dashboardService.actividadDiaria(1);

        // Then
        assertThat(actualActividad).hasSize(1);
        assertThat(actualActividad.get(0).fecha()).isEqualTo(hoy);
        assertThat(actualActividad.get(0).ingresos()).isEqualTo(1L);
    }

    @Test
    void actividadDiaria_fechaDevueltaComoTexto_seParseaComoLocalDate() {
        // Given
        LocalDate hoy = LocalDate.now();
        Object[] filaIngreso = { hoy.toString(), 1L, new BigDecimal("10.00"), new BigDecimal("20.00") };
        when(ingresoRepository.resumenDiario(any())).thenReturn(List.<Object[]>of(filaIngreso));
        when(entregaRepository.resumenDiario(any())).thenReturn(List.of());

        // When
        var actualActividad = dashboardService.actividadDiaria(1);

        // Then
        assertThat(actualActividad).hasSize(1);
        assertThat(actualActividad.get(0).fecha()).isEqualTo(hoy);
        assertThat(actualActividad.get(0).ingresos()).isEqualTo(1L);
    }

    @Test
    void actividadDiaria_pesoDevueltoComoEnteroNoBigDecimal_seConvierteABigDecimal() {
        // Given
        LocalDate hoy = LocalDate.now();
        Object[] filaIngreso = { hoy, 1L, 120, 500 };
        when(ingresoRepository.resumenDiario(any())).thenReturn(List.<Object[]>of(filaIngreso));
        when(entregaRepository.resumenDiario(any())).thenReturn(List.of());

        // When
        var actualActividad = dashboardService.actividadDiaria(1);

        // Then
        assertThat(actualActividad.get(0).pesoIngresadoKg()).isEqualByComparingTo("120");
        assertThat(actualActividad.get(0).valorIngresos()).isEqualByComparingTo("500");
    }
}
