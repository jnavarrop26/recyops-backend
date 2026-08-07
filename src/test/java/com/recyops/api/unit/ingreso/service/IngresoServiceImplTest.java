package com.recyops.api.unit.ingreso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.ingreso.dtos.CuerpoDetalleIngreso;
import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.CuerpoPago;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import com.recyops.api.ingreso.entity.IngresoMaterial;
import com.recyops.api.ingreso.enums.EstadoIngreso;
import com.recyops.api.ingreso.enums.EstadoPago;
import com.recyops.api.ingreso.enums.MetodoPago;
import com.recyops.api.ingreso.excepciones.IngresoNoEncontradoException;
import com.recyops.api.ingreso.repository.IngresoMaterialRepository;
import com.recyops.api.ingreso.service.IngresoServiceImpl;
import com.recyops.api.material.entity.Material;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.repository.MaterialRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class IngresoServiceImplTest {

    @Mock
    private IngresoMaterialRepository ingresoRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private IngresoServiceImpl ingresoService;

    @Test
    void historial_conFechas_convierteRangoYDelegaEnRepositorio() {
        // Given
        var fechaDesde = LocalDate.of(2026, 1, 1);
        var fechaHasta = LocalDate.of(2026, 1, 31);
        var ingreso = ingresoBase();
        Page<IngresoMaterial> pagina = new PageImpl<>(List.of(ingreso), PageRequest.of(0, 20), 1);
        var captorDesde = ArgumentCaptor.forClass(LocalDateTime.class);
        var captorHasta = ArgumentCaptor.forClass(LocalDateTime.class);
        when(ingresoRepository.buscarPorRango(captorDesde.capture(), captorHasta.capture(), any(Pageable.class)))
                .thenReturn(pagina);

        // When
        RespuestaPagina<RespuestaIngreso> actualResultado =
                ingresoService.historial(fechaDesde, fechaHasta, 0, 20);

        // Then
        assertThat(actualResultado.content()).hasSize(1);
        assertThat(captorDesde.getValue()).isEqualTo(fechaDesde.atStartOfDay());
        assertThat(captorHasta.getValue()).isEqualTo(fechaHasta.atTime(LocalTime.MAX));
    }

    @Test
    void historial_sinFechas_noAplicaFiltroDeRango() {
        // Given
        Page<IngresoMaterial> paginaVacia = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        var captorDesde = ArgumentCaptor.forClass(LocalDateTime.class);
        var captorHasta = ArgumentCaptor.forClass(LocalDateTime.class);
        when(ingresoRepository.buscarPorRango(captorDesde.capture(), captorHasta.capture(), any(Pageable.class)))
                .thenReturn(paginaVacia);

        // When
        ingresoService.historial(null, null, 0, 20);

        // Then
        assertThat(captorDesde.getValue()).isNull();
        assertThat(captorHasta.getValue()).isNull();
    }

    @Test
    void obtener_idExistente_retornaIngresoConDetalles() {
        // Given
        var ingreso = ingresoBase();
        when(ingresoRepository.findById(1L)).thenReturn(Optional.of(ingreso));

        // When
        RespuestaIngreso actualIngreso = ingresoService.obtener(1L);

        // Then
        assertThat(actualIngreso.cliente()).isEqualTo("Juan Perez");
    }

    @Test
    void obtener_idInexistente_lanzaIngresoNoEncontradoException() {
        // Given
        when(ingresoRepository.findById(99L)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> ingresoService.obtener(99L))
                .isInstanceOf(IngresoNoEncontradoException.class);
    }

    @Test
    void obtenerPorUuid_uuidExistente_retornaIngresoConDetalles() {
        // Given
        var uuid = UUID.randomUUID();
        var ingreso = ingresoBase();
        when(ingresoRepository.findByUuid(uuid)).thenReturn(Optional.of(ingreso));

        // When
        RespuestaIngreso actualIngreso = ingresoService.obtenerPorUuid(uuid);

        // Then
        assertThat(actualIngreso.cliente()).isEqualTo("Juan Perez");
    }

    @Test
    void obtenerPorUuid_uuidInexistente_lanzaIngresoNoEncontradoException() {
        // Given
        var uuid = UUID.randomUUID();
        when(ingresoRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> ingresoService.obtenerPorUuid(uuid))
                .isInstanceOf(IngresoNoEncontradoException.class);
    }

    @Test
    void registrar_sinMateriales_usaTotalesDelCuerpo() {
        // Given
        var cuerpo = new CuerpoIngreso("Juan Perez", "123456", "Bodega Central", "Ana",
                "ABC123", new BigDecimal("50"), new BigDecimal("100000"), null);
        when(ingresoRepository.save(any(IngresoMaterial.class))).thenAnswer(inv -> inv.getArgument(0));
        var captor = ArgumentCaptor.forClass(IngresoMaterial.class);

        // When
        ingresoService.registrar(cuerpo);

        // Then
        verify(ingresoRepository).save(captor.capture());
        var savedIngreso = captor.getValue();
        assertThat(savedIngreso.getPesoNetoTotal()).isEqualByComparingTo("50");
        assertThat(savedIngreso.getTotal()).isEqualByComparingTo("100000");
        assertThat(savedIngreso.getDetalles()).isEmpty();
        verify(materialRepository, never()).findById(any());
    }

    @Test
    void registrar_conMaterialIdValido_calculaTotalesDesdeCatalogo() {
        // Given
        var materialId = UUID.randomUUID();
        var material = Material.builder().id(materialId).nombre("PET").precioBase(new BigDecimal("4000")).build();
        var detalle = new CuerpoDetalleIngreso(materialId, null,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5000"), null);
        var cuerpo = new CuerpoIngreso("Juan Perez", "123456", "Bodega Central", "Ana",
                "ABC123", new BigDecimal("0"), new BigDecimal("0"), List.of(detalle));
        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(ingresoRepository.save(any(IngresoMaterial.class))).thenAnswer(inv -> inv.getArgument(0));
        var captor = ArgumentCaptor.forClass(IngresoMaterial.class);

        // When
        ingresoService.registrar(cuerpo);

        // Then
        verify(ingresoRepository).save(captor.capture());
        var savedIngreso = captor.getValue();
        assertThat(savedIngreso.getPesoNetoTotal()).isEqualByComparingTo("90");
        assertThat(savedIngreso.getTotal()).isEqualByComparingTo("450000");
        var savedDetalle = savedIngreso.getDetalles().get(0);
        assertThat(savedDetalle.getCategoria()).isEqualTo("PET");
        assertThat(savedDetalle.getPesoNeto()).isEqualByComparingTo("90");
        assertThat(savedDetalle.getSubtotal()).isEqualByComparingTo("450000");
    }

    @Test
    void registrar_conMaterialIdInexistente_lanzaMaterialNoEncontradoException() {
        // Given
        var materialId = UUID.randomUUID();
        var detalle = new CuerpoDetalleIngreso(materialId, null,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5000"), null);
        var cuerpo = new CuerpoIngreso("Juan Perez", "123456", "Bodega Central", "Ana",
                "ABC123", new BigDecimal("0"), new BigDecimal("0"), List.of(detalle));
        when(materialRepository.findById(materialId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> ingresoService.registrar(cuerpo))
                .isInstanceOf(MaterialNoEncontradoException.class);
    }

    @Test
    void registrar_conCategoriaLibreSinMaterialId_usaCategoriaDelCuerpo() {
        // Given
        var detalle = new CuerpoDetalleIngreso(null, "Carton",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("1000"), null);
        var cuerpo = new CuerpoIngreso("Juan Perez", "123456", "Bodega Central", "Ana",
                "ABC123", new BigDecimal("0"), new BigDecimal("0"), List.of(detalle));
        when(ingresoRepository.save(any(IngresoMaterial.class))).thenAnswer(inv -> inv.getArgument(0));
        var captor = ArgumentCaptor.forClass(IngresoMaterial.class);

        // When
        ingresoService.registrar(cuerpo);

        // Then
        verify(ingresoRepository).save(captor.capture());
        var savedDetalle = captor.getValue().getDetalles().get(0);
        assertThat(savedDetalle.getCategoria()).isEqualTo("Carton");
        assertThat(savedDetalle.getPrecioKilo()).isEqualByComparingTo("1000");
    }

    @Test
    void registrar_sinMaterialIdNiCategoria_lanzaReglaNegocioException() {
        // Given
        var detalle = new CuerpoDetalleIngreso(null, null,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("1000"), null);
        var cuerpo = new CuerpoIngreso("Juan Perez", "123456", "Bodega Central", "Ana",
                "ABC123", new BigDecimal("0"), new BigDecimal("0"), List.of(detalle));

        // When-Then
        assertThatThrownBy(() -> ingresoService.registrar(cuerpo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("materialId o categoria");
    }

    @Test
    void registrar_sinPrecioKiloNiMaterialConPrecio_lanzaReglaNegocioException() {
        // Given
        var detalle = new CuerpoDetalleIngreso(null, "Carton",
                new BigDecimal("100"), new BigDecimal("10"), null, null);
        var cuerpo = new CuerpoIngreso("Juan Perez", "123456", "Bodega Central", "Ana",
                "ABC123", new BigDecimal("0"), new BigDecimal("0"), List.of(detalle));

        // When-Then
        assertThatThrownBy(() -> ingresoService.registrar(cuerpo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("precio por kilo");
    }

    @Test
    void registrar_pesoBrutoMenorQueTara_pesoNetoQuedaEnCero() {
        // Given
        var detalle = new CuerpoDetalleIngreso(null, "Carton",
                new BigDecimal("5"), new BigDecimal("20"), new BigDecimal("1000"), null);
        var cuerpo = new CuerpoIngreso("Juan Perez", "123456", "Bodega Central", "Ana",
                "ABC123", new BigDecimal("0"), new BigDecimal("0"), List.of(detalle));
        when(ingresoRepository.save(any(IngresoMaterial.class))).thenAnswer(inv -> inv.getArgument(0));
        var captor = ArgumentCaptor.forClass(IngresoMaterial.class);

        // When
        ingresoService.registrar(cuerpo);

        // Then
        verify(ingresoRepository).save(captor.capture());
        var savedDetalle = captor.getValue().getDetalles().get(0);
        assertThat(savedDetalle.getPesoNeto()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registrarPago_idInexistente_lanzaIngresoNoEncontradoException() {
        // Given
        when(ingresoRepository.findById(1L)).thenReturn(Optional.empty());
        var cuerpo = new CuerpoPago(MetodoPago.EFECTIVO);

        // When-Then
        assertThatThrownBy(() -> ingresoService.registrarPago(1L, cuerpo))
                .isInstanceOf(IngresoNoEncontradoException.class);
    }

    @Test
    void registrarPago_yaPagadoConMismoMetodo_lanzaReglaNegocioException() {
        // Given
        var ingreso = IngresoMaterial.builder()
                .cliente("Juan Perez").cedula("123456").bodegaDestino("Bodega Central").encargado("Ana")
                .pesoNetoTotal(new BigDecimal("10")).total(new BigDecimal("1000"))
                .estadoPago(EstadoPago.PAGADO).metodoPago(MetodoPago.EFECTIVO)
                .build();
        when(ingresoRepository.findById(1L)).thenReturn(Optional.of(ingreso));
        var cuerpo = new CuerpoPago(MetodoPago.EFECTIVO);

        // When-Then
        assertThatThrownBy(() -> ingresoService.registrarPago(1L, cuerpo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya fue pagado");
    }

    @Test
    void registrarPago_pendienteDePago_marcaComoPagado() {
        // Given
        var ingreso = IngresoMaterial.builder()
                .cliente("Juan Perez").cedula("123456").bodegaDestino("Bodega Central").encargado("Ana")
                .pesoNetoTotal(new BigDecimal("10")).total(new BigDecimal("1000"))
                .build();
        when(ingresoRepository.findById(1L)).thenReturn(Optional.of(ingreso));
        var cuerpo = new CuerpoPago(MetodoPago.TRANSFERENCIA);

        // When
        RespuestaIngreso actualResultado = ingresoService.registrarPago(1L, cuerpo);

        // Then
        assertThat(actualResultado.estadoPago()).isEqualTo("PAGADO");
        assertThat(actualResultado.metodoPago()).isEqualTo("TRANSFERENCIA");
    }

    @Test
    void registrarPago_yaPagadoConMetodoDistinto_corrigeMetodoDePago() {
        // Given
        var ingreso = IngresoMaterial.builder()
                .cliente("Juan Perez").cedula("123456").bodegaDestino("Bodega Central").encargado("Ana")
                .pesoNetoTotal(new BigDecimal("10")).total(new BigDecimal("1000"))
                .estadoPago(EstadoPago.PAGADO).metodoPago(MetodoPago.EFECTIVO)
                .build();
        when(ingresoRepository.findById(1L)).thenReturn(Optional.of(ingreso));
        var cuerpo = new CuerpoPago(MetodoPago.TRANSFERENCIA);

        // When
        RespuestaIngreso actualResultado = ingresoService.registrarPago(1L, cuerpo);

        // Then
        assertThat(actualResultado.metodoPago()).isEqualTo("TRANSFERENCIA");
    }

    @Test
    void cambiarEstado_idInexistente_lanzaIngresoNoEncontradoException() {
        // Given
        when(ingresoRepository.findById(1L)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> ingresoService.cambiarEstado(1L, EstadoIngreso.EN_BODEGA))
                .isInstanceOf(IngresoNoEncontradoException.class);
    }

    @Test
    void cambiarEstado_idExistente_actualizaEstado() {
        // Given
        var ingreso = ingresoBase();
        when(ingresoRepository.findById(1L)).thenReturn(Optional.of(ingreso));

        // When
        RespuestaIngreso actualResultado = ingresoService.cambiarEstado(1L, EstadoIngreso.EN_BODEGA);

        // Then
        assertThat(actualResultado.estado()).isEqualTo("EN_BODEGA");
    }

    @Test
    void cambiarPaso_idInexistente_lanzaIngresoNoEncontradoException() {
        // Given
        when(ingresoRepository.findById(1L)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> ingresoService.cambiarPaso(1L, true))
                .isInstanceOf(IngresoNoEncontradoException.class);
    }

    @Test
    void cambiarPaso_idExistente_actualizaPaso() {
        // Given
        var ingreso = ingresoBase();
        when(ingresoRepository.findById(1L)).thenReturn(Optional.of(ingreso));

        // When
        RespuestaIngreso actualResultado = ingresoService.cambiarPaso(1L, true);

        // Then
        assertThat(actualResultado.paso()).isTrue();
    }

    private IngresoMaterial ingresoBase() {
        return IngresoMaterial.builder()
                .cliente("Juan Perez")
                .cedula("123456")
                .bodegaDestino("Bodega Central")
                .encargado("Ana")
                .placaVehiculo("ABC123")
                .pesoNetoTotal(new BigDecimal("50"))
                .total(new BigDecimal("100000"))
                .build();
    }
}
