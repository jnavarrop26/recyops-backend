package com.recyops.api.unit.entrega.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.entrega.dtos.CuerpoEntrega;
import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import com.recyops.api.entrega.excepciones.EntregaNoEncontradaException;
import com.recyops.api.entrega.excepciones.TransicionEstadoInvalidaException;
import com.recyops.api.entrega.repository.EntregaRepository;
import com.recyops.api.entrega.service.EntregaServiceImpl;
import com.recyops.api.entrega.service.GeneradorReciboPdf;
import com.recyops.api.inventario.interfaces.InventarioService;
import com.recyops.api.material.entity.Material;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.repository.MaterialRepository;
import com.recyops.api.proveedor.entity.Proveedor;
import com.recyops.api.proveedor.excepciones.ProveedorNoEncontradoException;
import com.recyops.api.proveedor.repository.ProveedorRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * GeneradorReciboPdf (uso de PDFBox) se mockea como colaborador: el unit test
 * no ejercita generacion real de PDF, solo verifica que EntregaServiceImpl
 * llama al generador con la entrega correcta y propaga su resultado.
 */
@ExtendWith(MockitoExtension.class)
class EntregaServiceImplTest {

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private GeneradorReciboPdf generadorReciboPdf;

    @InjectMocks
    private EntregaServiceImpl entregaService;

    // ---- listar ----

    @Test
    void listar_conFechasProvistas_convierteRangoALocalDateTime() {
        // Given
        var bodegaId = UUID.randomUUID();
        var proveedorId = UUID.randomUUID();
        var fechaDesde = LocalDate.of(2026, 1, 1);
        var fechaHasta = LocalDate.of(2026, 1, 31);
        var pagina = new PageImpl<Entrega>(List.of());
        var captorDesde = ArgumentCaptor.forClass(LocalDateTime.class);
        var captorHasta = ArgumentCaptor.forClass(LocalDateTime.class);
        when(entregaRepository.buscar(eq(bodegaId), eq(proveedorId), eq(EstadoEntrega.RECIBIDA),
                captorDesde.capture(), captorHasta.capture(), eq(PageRequest.of(0, 20))))
                .thenReturn(pagina);

        // When
        entregaService.listar(bodegaId, proveedorId, EstadoEntrega.RECIBIDA, fechaDesde, fechaHasta, 0, 20);

        // Then
        assertThat(captorDesde.getValue()).isEqualTo(fechaDesde.atStartOfDay());
        assertThat(captorHasta.getValue()).isEqualTo(fechaHasta.atTime(LocalTime.MAX));
    }

    @Test
    void listar_sinFechasProvistas_pasaRangoNulo() {
        // Given
        var pagina = new PageImpl<Entrega>(List.of());
        when(entregaRepository.buscar(isNull(), isNull(), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 20))))
                .thenReturn(pagina);

        // When
        var actualPagina = entregaService.listar(null, null, null, null, null, 0, 20);

        // Then
        assertThat(actualPagina.content()).isEmpty();
    }

    // ---- obtener ----

    @Test
    void obtener_entregaExistente_retornaRespuestaEntrega() {
        // Given
        var entrega = entregaCompleta(EstadoEntrega.RECIBIDA);
        when(entregaRepository.findById(entrega.getId())).thenReturn(Optional.of(entrega));

        // When
        var actualRespuesta = entregaService.obtener(entrega.getId());

        // Then
        assertThat(actualRespuesta.codigo()).isEqualTo(entrega.getCodigo());
        assertThat(actualRespuesta.proveedorNombre()).isEqualTo(entrega.getProveedor().getNombre());
        assertThat(actualRespuesta.bodegaNombre()).isEqualTo(entrega.getBodega().getNombre());
    }

    @Test
    void obtener_entregaInexistente_lanzaEntregaNoEncontradaException() {
        // Given
        var id = UUID.randomUUID();
        when(entregaRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> entregaService.obtener(id))
                .isInstanceOf(EntregaNoEncontradaException.class);
    }

    // ---- registrar ----

    @Test
    void registrar_conFechaRecepcionProvista_guardaEntregaYRegistraEntradaEnInventario() {
        // Given
        var proveedor = Proveedor.builder().id(UUID.randomUUID()).nombre("Proveedor Uno").build();
        var bodega = Bodega.builder().id(UUID.randomUUID()).nombre("Bodega Central").build();
        var material = Material.builder().id(UUID.randomUUID()).nombre("PET transparente").build();
        var fechaRecepcion = LocalDateTime.of(2026, 3, 1, 10, 0);
        var cuerpo = new CuerpoEntrega(proveedor.getId(), bodega.getId(), material.getId(),
                new BigDecimal("50.00"), "Juan Perez", fechaRecepcion);
        when(proveedorRepository.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        when(bodegaRepository.findById(bodega.getId())).thenReturn(Optional.of(bodega));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(entregaRepository.siguienteConsecutivo()).thenReturn(42L);
        var captorEntrega = ArgumentCaptor.forClass(Entrega.class);
        when(entregaRepository.save(captorEntrega.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        var actualRespuesta = entregaService.registrar(cuerpo);

        // Then
        var entregaGuardada = captorEntrega.getValue();
        assertThat(entregaGuardada.getCodigo()).isEqualTo("ENT-000042");
        assertThat(entregaGuardada.getFechaRecepcion()).isEqualTo(fechaRecepcion);
        assertThat(actualRespuesta.codigo()).isEqualTo("ENT-000042");
        verify(inventarioService).registrarEntrada(bodega.getId(), material.getId(),
                new BigDecimal("50.00"), "ENT-000042");
    }

    @Test
    void registrar_sinFechaRecepcionProvista_usaFechaActual() {
        // Given
        var proveedor = Proveedor.builder().id(UUID.randomUUID()).nombre("Proveedor Uno").build();
        var bodega = Bodega.builder().id(UUID.randomUUID()).nombre("Bodega Central").build();
        var material = Material.builder().id(UUID.randomUUID()).nombre("PET transparente").build();
        var cuerpo = new CuerpoEntrega(proveedor.getId(), bodega.getId(), material.getId(),
                new BigDecimal("50.00"), "Juan Perez", null);
        when(proveedorRepository.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        when(bodegaRepository.findById(bodega.getId())).thenReturn(Optional.of(bodega));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(entregaRepository.siguienteConsecutivo()).thenReturn(1L);
        var captorEntrega = ArgumentCaptor.forClass(Entrega.class);
        when(entregaRepository.save(captorEntrega.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        entregaService.registrar(cuerpo);

        // Then
        assertThat(captorEntrega.getValue().getFechaRecepcion()).isNotNull();
    }

    @Test
    void registrar_proveedorInexistente_lanzaProveedorNoEncontradoException() {
        // Given
        var cuerpo = new CuerpoEntrega(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "Juan Perez", null);
        when(proveedorRepository.findById(cuerpo.proveedorId())).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> entregaService.registrar(cuerpo))
                .isInstanceOf(ProveedorNoEncontradoException.class);
    }

    @Test
    void registrar_bodegaInexistente_lanzaBodegaNoEncontradaException() {
        // Given
        var proveedor = Proveedor.builder().id(UUID.randomUUID()).nombre("Proveedor Uno").build();
        var cuerpo = new CuerpoEntrega(proveedor.getId(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "Juan Perez", null);
        when(proveedorRepository.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        when(bodegaRepository.findById(cuerpo.bodegaId())).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> entregaService.registrar(cuerpo))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    @Test
    void registrar_materialInexistente_lanzaMaterialNoEncontradoException() {
        // Given
        var proveedor = Proveedor.builder().id(UUID.randomUUID()).nombre("Proveedor Uno").build();
        var bodega = Bodega.builder().id(UUID.randomUUID()).nombre("Bodega Central").build();
        var cuerpo = new CuerpoEntrega(proveedor.getId(), bodega.getId(), UUID.randomUUID(),
                new BigDecimal("50.00"), "Juan Perez", null);
        when(proveedorRepository.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        when(bodegaRepository.findById(bodega.getId())).thenReturn(Optional.of(bodega));
        when(materialRepository.findById(cuerpo.tipoMaterialId())).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> entregaService.registrar(cuerpo))
                .isInstanceOf(MaterialNoEncontradoException.class);
    }

    // ---- cambiarEstado ----

    @Test
    void cambiarEstado_transicionValida_actualizaEstadoSinTocarInventario() {
        // Given
        var entrega = entregaCompleta(EstadoEntrega.RECIBIDA);
        when(entregaRepository.findById(entrega.getId())).thenReturn(Optional.of(entrega));

        // When
        var actualRespuesta = entregaService.cambiarEstado(entrega.getId(), EstadoEntrega.EN_PROCESO);

        // Then
        assertThat(actualRespuesta.estado()).isEqualTo(EstadoEntrega.EN_PROCESO);
        verifyNoInteractions(inventarioService);
    }

    @Test
    void cambiarEstado_transicionInvalida_lanzaTransicionEstadoInvalidaException() {
        // Given
        var entrega = entregaCompleta(EstadoEntrega.RECIBIDA);
        when(entregaRepository.findById(entrega.getId())).thenReturn(Optional.of(entrega));

        // When-Then: RECIBIDA solo puede pasar a EN_PROCESO, no directo a PROCESADA
        assertThatThrownBy(() -> entregaService.cambiarEstado(entrega.getId(), EstadoEntrega.PROCESADA))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cambiarEstado_aDespachada_registraSalidaEnInventario() {
        // Given
        var entrega = entregaCompleta(EstadoEntrega.PROCESADA);
        when(entregaRepository.findById(entrega.getId())).thenReturn(Optional.of(entrega));

        // When
        var actualRespuesta = entregaService.cambiarEstado(entrega.getId(), EstadoEntrega.DESPACHADA);

        // Then
        assertThat(actualRespuesta.estado()).isEqualTo(EstadoEntrega.DESPACHADA);
        verify(inventarioService).registrarSalida(entrega.getBodega().getId(), entrega.getTipoMaterial().getId(),
                entrega.getPesoKg(), "Despacho " + entrega.getCodigo());
    }

    @Test
    void cambiarEstado_entregaInexistente_lanzaEntregaNoEncontradaException() {
        // Given
        var id = UUID.randomUUID();
        when(entregaRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> entregaService.cambiarEstado(id, EstadoEntrega.EN_PROCESO))
                .isInstanceOf(EntregaNoEncontradaException.class);
    }

    // ---- eliminar ----

    @Test
    void eliminar_entregaNoDespachada_reviertaEntradaEnInventarioYElimina() {
        // Given
        var entrega = entregaCompleta(EstadoEntrega.RECIBIDA);
        when(entregaRepository.findById(entrega.getId())).thenReturn(Optional.of(entrega));

        // When
        entregaService.eliminar(entrega.getId());

        // Then
        verify(inventarioService).registrarSalida(entrega.getBodega().getId(), entrega.getTipoMaterial().getId(),
                entrega.getPesoKg(), "Eliminacion " + entrega.getCodigo());
        verify(entregaRepository).delete(entrega);
    }

    @Test
    void eliminar_entregaDespachada_lanzaReglaNegocioException() {
        // Given
        var entrega = entregaCompleta(EstadoEntrega.DESPACHADA);
        when(entregaRepository.findById(entrega.getId())).thenReturn(Optional.of(entrega));

        // When-Then
        assertThatThrownBy(() -> entregaService.eliminar(entrega.getId()))
                .isInstanceOf(ReglaNegocioException.class);
        verify(entregaRepository, never()).delete(any());
    }

    @Test
    void eliminar_entregaInexistente_lanzaEntregaNoEncontradaException() {
        // Given
        var id = UUID.randomUUID();
        when(entregaRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> entregaService.eliminar(id))
                .isInstanceOf(EntregaNoEncontradaException.class);
    }

    // ---- generarRecibo ----

    @Test
    void generarRecibo_entregaExistente_retornaReciboConPdfGenerado() {
        // Given
        var entrega = entregaCompleta(EstadoEntrega.RECIBIDA);
        when(entregaRepository.buscarConRelaciones(entrega.getId())).thenReturn(Optional.of(entrega));
        byte[] pdfEsperado = { 1, 2, 3 };
        when(generadorReciboPdf.generar(entrega)).thenReturn(pdfEsperado);

        // When
        var actualRecibo = entregaService.generarRecibo(entrega.getId());

        // Then
        assertThat(actualRecibo.codigo()).isEqualTo(entrega.getCodigo());
        assertThat(actualRecibo.contenido()).isEqualTo(pdfEsperado);
    }

    @Test
    void generarRecibo_entregaInexistente_lanzaEntregaNoEncontradaException() {
        // Given
        var id = UUID.randomUUID();
        when(entregaRepository.buscarConRelaciones(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> entregaService.generarRecibo(id))
                .isInstanceOf(EntregaNoEncontradaException.class);
        verifyNoInteractions(generadorReciboPdf);
    }

    // ---- datos de prueba ----

    private Entrega entregaCompleta(EstadoEntrega estado) {
        var proveedor = Proveedor.builder().id(UUID.randomUUID()).nombre("Proveedor Uno").build();
        var bodega = Bodega.builder().id(UUID.randomUUID()).nombre("Bodega Central").build();
        var material = Material.builder().id(UUID.randomUUID()).nombre("PET transparente").build();
        return Entrega.builder()
                .id(UUID.randomUUID())
                .codigo("ENT-000001")
                .proveedor(proveedor)
                .bodega(bodega)
                .tipoMaterial(material)
                .pesoKg(new BigDecimal("50.00"))
                .personaEntrega("Juan Perez")
                .estado(estado)
                .fechaRecepcion(LocalDateTime.now())
                .usuarioRegistroNombre("sistema")
                .build();
    }
}
