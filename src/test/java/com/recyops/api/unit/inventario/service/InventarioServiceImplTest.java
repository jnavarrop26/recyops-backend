package com.recyops.api.unit.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.UsuarioAutenticado;
import com.recyops.api.inventario.dtos.CuerpoAjuste;
import com.recyops.api.inventario.dtos.CuerpoCrearLinea;
import com.recyops.api.inventario.dtos.CuerpoMerma;
import com.recyops.api.inventario.dtos.CuerpoTopes;
import com.recyops.api.inventario.entity.LineaInventario;
import com.recyops.api.inventario.entity.MovimientoInventario;
import com.recyops.api.inventario.enums.TipoOperacion;
import com.recyops.api.inventario.excepciones.LineaDuplicadaException;
import com.recyops.api.inventario.excepciones.LineaInventarioNoEncontradaException;
import com.recyops.api.inventario.excepciones.StockInvalidoException;
import com.recyops.api.inventario.repository.LineaInventarioRepository;
import com.recyops.api.inventario.repository.MovimientoInventarioRepository;
import com.recyops.api.inventario.service.InventarioServiceImpl;
import com.recyops.api.material.entity.Material;
import com.recyops.api.material.entity.OpcionCatalogo;
import com.recyops.api.material.enums.TipoOpcionCatalogo;
import com.recyops.api.material.enums.UnidadMedida;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.repository.MaterialRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock
    private LineaInventarioRepository lineaRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    // ---------- registrarEntrada ----------

    @Test
    void registrarEntrada_lineaExistente_incrementaStockYRegistraMovimiento() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"));
        when(lineaRepository.findByBodegaIdAndTipoMaterialId(bodega.getId(), material.getId()))
                .thenReturn(Optional.of(linea));
        var captorMovimiento = ArgumentCaptor.forClass(MovimientoInventario.class);
        when(movimientoRepository.save(captorMovimiento.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Sistema Entregas");

            // When
            inventarioService.registrarEntrada(bodega.getId(), material.getId(), new BigDecimal("50.00"),
                    "ENTREGA-1");

            // Then
            assertThat(linea.getStockActual()).isEqualByComparingTo("150.00");
            var movimientoGuardado = captorMovimiento.getValue();
            assertThat(movimientoGuardado.getTipoOperacion()).isEqualTo(TipoOperacion.ENTRADA);
            assertThat(movimientoGuardado.getCantidad()).isEqualByComparingTo("50.00");
            assertThat(movimientoGuardado.getCantidadAnterior()).isEqualByComparingTo("100.00");
            assertThat(movimientoGuardado.getCantidadNueva()).isEqualByComparingTo("150.00");
            assertThat(movimientoGuardado.getReferencia()).isEqualTo("ENTREGA-1");
            assertThat(movimientoGuardado.getUsuarioNombre()).isEqualTo("Sistema Entregas");
        }
    }

    @Test
    void registrarEntrada_lineaInexistente_creaLineaAutomaticaConTopesEnCero() {
        // Given
        var bodegaId = UUID.randomUUID();
        var tipoMaterialId = UUID.randomUUID();
        var bodega = crearBodega(bodegaId, "Bodega Sur");
        var material = crearMaterial(tipoMaterialId, "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        when(lineaRepository.findByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)).thenReturn(Optional.empty());
        when(bodegaRepository.findById(bodegaId)).thenReturn(Optional.of(bodega));
        when(materialRepository.findById(tipoMaterialId)).thenReturn(Optional.of(material));
        var captorLinea = ArgumentCaptor.forClass(LineaInventario.class);
        when(lineaRepository.save(captorLinea.capture())).thenAnswer(inv -> inv.getArgument(0));
        var captorMovimiento = ArgumentCaptor.forClass(MovimientoInventario.class);
        when(movimientoRepository.save(captorMovimiento.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Sistema Entregas");

            // When
            inventarioService.registrarEntrada(bodegaId, tipoMaterialId, new BigDecimal("20.00"), "ENTREGA-2");

            // Then
            var lineaCreada = captorLinea.getValue();
            assertThat(lineaCreada.getBodega()).isEqualTo(bodega);
            assertThat(lineaCreada.getTipoMaterial()).isEqualTo(material);
            assertThat(lineaCreada.getStockMinimo()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(lineaCreada.getStockMaximo()).isEqualByComparingTo(BigDecimal.ZERO);
            var movimientoGuardado = captorMovimiento.getValue();
            assertThat(movimientoGuardado.getCantidadAnterior()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(movimientoGuardado.getCantidadNueva()).isEqualByComparingTo("20.00");
        }
    }

    @Test
    void registrarEntrada_bodegaInexistenteEnCreacionAutomatica_lanzaBodegaNoEncontradaException() {
        // Given
        var bodegaId = UUID.randomUUID();
        var tipoMaterialId = UUID.randomUUID();
        when(lineaRepository.findByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)).thenReturn(Optional.empty());
        when(bodegaRepository.findById(bodegaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.registrarEntrada(bodegaId, tipoMaterialId, BigDecimal.TEN, "REF"))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    @Test
    void registrarEntrada_materialInexistenteEnCreacionAutomatica_lanzaMaterialNoEncontradoException() {
        // Given
        var bodegaId = UUID.randomUUID();
        var tipoMaterialId = UUID.randomUUID();
        var bodega = crearBodega(bodegaId, "Bodega Sur");
        when(lineaRepository.findByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)).thenReturn(Optional.empty());
        when(bodegaRepository.findById(bodegaId)).thenReturn(Optional.of(bodega));
        when(materialRepository.findById(tipoMaterialId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.registrarEntrada(bodegaId, tipoMaterialId, BigDecimal.TEN, "REF"))
                .isInstanceOf(MaterialNoEncontradoException.class);
    }

    // ---------- registrarSalida ----------

    @Test
    void registrarSalida_stockSuficiente_descuentaStockYRegistraMovimiento() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"));
        when(lineaRepository.findByBodegaIdAndTipoMaterialId(bodega.getId(), material.getId()))
                .thenReturn(Optional.of(linea));
        var captorMovimiento = ArgumentCaptor.forClass(MovimientoInventario.class);
        when(movimientoRepository.save(captorMovimiento.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Sistema Despachos");

            // When
            inventarioService.registrarSalida(bodega.getId(), material.getId(), new BigDecimal("30.00"),
                    "DESPACHO-1");

            // Then
            assertThat(linea.getStockActual()).isEqualByComparingTo("70.00");
            var movimientoGuardado = captorMovimiento.getValue();
            assertThat(movimientoGuardado.getTipoOperacion()).isEqualTo(TipoOperacion.SALIDA);
            assertThat(movimientoGuardado.getCantidadAnterior()).isEqualByComparingTo("100.00");
            assertThat(movimientoGuardado.getCantidadNueva()).isEqualByComparingTo("70.00");
        }
    }

    @Test
    void registrarSalida_lineaInexistente_lanzaStockInvalidoException() {
        // Given
        var bodegaId = UUID.randomUUID();
        var tipoMaterialId = UUID.randomUUID();
        when(lineaRepository.findByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.registrarSalida(bodegaId, tipoMaterialId, BigDecimal.TEN, "REF"))
                .isInstanceOf(StockInvalidoException.class);
    }

    @Test
    void registrarSalida_stockInsuficiente_lanzaStockInvalidoException() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("10.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"));
        when(lineaRepository.findByBodegaIdAndTipoMaterialId(bodega.getId(), material.getId()))
                .thenReturn(Optional.of(linea));

        // When-Then
        assertThatThrownBy(() -> inventarioService.registrarSalida(bodega.getId(), material.getId(),
                new BigDecimal("50.00"), "DESPACHO-2"))
                .isInstanceOf(StockInvalidoException.class);
        verify(movimientoRepository, never()).save(any());
    }

    // ---------- listar ----------

    @Test
    void listar_conResultados_devuelvePaginaMapeada() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("5.00"), new BigDecimal("10.00"),
                new BigDecimal("100.00"));
        Page<LineaInventario> pagina = new PageImpl<>(List.of(linea), PageRequest.of(0, 10), 1);
        when(lineaRepository.buscar(eq(bodega.getId()), eq(material.getId()), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        // When
        var actualPagina = inventarioService.listar(bodega.getId(), material.getId(), true, 0, 10);

        // Then
        assertThat(actualPagina.content()).hasSize(1);
        var actualLinea = actualPagina.content().get(0);
        assertThat(actualLinea.bodegaNombre()).isEqualTo("Bodega Sur");
        assertThat(actualLinea.tipoMaterialNombre()).isEqualTo("PET");
        assertThat(actualLinea.categoriaNombre()).isEqualTo("Plasticos");
        assertThat(actualLinea.unidadMedida()).isEqualTo("KILOGRAMO");
        assertThat(actualLinea.bajoMinimo()).isTrue();
        assertThat(actualPagina.totalElements()).isEqualTo(1);
    }

    @Test
    void listar_sinResultados_devuelvePaginaVacia() {
        // Given
        Page<LineaInventario> paginaVacia = Page.empty(PageRequest.of(0, 10));
        when(lineaRepository.buscar(any(), any(), any(), any(Pageable.class))).thenReturn(paginaVacia);

        // When
        var actualPagina = inventarioService.listar(null, null, null, 0, 10);

        // Then
        assertThat(actualPagina.content()).isEmpty();
        assertThat(actualPagina.totalElements()).isZero();
    }

    // ---------- obtener ----------

    @Test
    void obtener_lineaExistente_devuelveLinea() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("50.00"), BigDecimal.ZERO,
                new BigDecimal("100.00"));
        when(lineaRepository.findById(linea.getId())).thenReturn(Optional.of(linea));

        // When
        var actualLinea = inventarioService.obtener(linea.getId());

        // Then
        assertThat(actualLinea.id()).isEqualTo(linea.getId());
        assertThat(actualLinea.stockActual()).isEqualByComparingTo("50.00");
    }

    @Test
    void obtener_lineaInexistente_lanzaLineaInventarioNoEncontradaException() {
        // Given
        var lineaId = UUID.randomUUID();
        when(lineaRepository.findById(lineaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.obtener(lineaId))
                .isInstanceOf(LineaInventarioNoEncontradaException.class);
    }

    // ---------- movimientos ----------

    @Test
    void movimientos_lineaExistente_devuelvePaginaDeMovimientos() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("50.00"), BigDecimal.ZERO,
                new BigDecimal("100.00"));
        var movimiento = MovimientoInventario.builder()
                .id(UUID.randomUUID())
                .linea(linea)
                .tipoOperacion(TipoOperacion.ENTRADA)
                .cantidad(new BigDecimal("10.00"))
                .cantidadAnterior(new BigDecimal("40.00"))
                .cantidadNueva(new BigDecimal("50.00"))
                .referencia("ENTREGA-3")
                .usuarioNombre("Admin")
                .build();
        Page<MovimientoInventario> pagina = new PageImpl<>(List.of(movimiento), PageRequest.of(0, 10), 1);
        when(lineaRepository.findById(linea.getId())).thenReturn(Optional.of(linea));
        when(movimientoRepository.findByLineaIdOrderByFechaRegistroDesc(eq(linea.getId()), any(Pageable.class)))
                .thenReturn(pagina);

        // When
        var actualPagina = inventarioService.movimientos(linea.getId(), 0, 10);

        // Then
        assertThat(actualPagina.content()).hasSize(1);
        assertThat(actualPagina.content().get(0).referencia()).isEqualTo("ENTREGA-3");
    }

    @Test
    void movimientos_lineaInexistente_lanzaLineaInventarioNoEncontradaException() {
        // Given
        var lineaId = UUID.randomUUID();
        when(lineaRepository.findById(lineaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.movimientos(lineaId, 0, 10))
                .isInstanceOf(LineaInventarioNoEncontradaException.class);
        verify(movimientoRepository, never()).findByLineaIdOrderByFechaRegistroDesc(any(), any());
    }

    // ---------- crearLinea ----------

    @Test
    void crearLinea_datosValidos_creaLinea() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var cuerpo = new CuerpoCrearLinea(bodega.getId(), material.getId(), new BigDecimal("10.00"),
                new BigDecimal("100.00"));
        when(lineaRepository.existsByBodegaIdAndTipoMaterialId(bodega.getId(), material.getId())).thenReturn(false);
        when(bodegaRepository.findById(bodega.getId())).thenReturn(Optional.of(bodega));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        var captorLinea = ArgumentCaptor.forClass(LineaInventario.class);
        when(lineaRepository.save(captorLinea.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        var actualLinea = inventarioService.crearLinea(cuerpo);

        // Then
        assertThat(actualLinea.bodegaId()).isEqualTo(bodega.getId());
        assertThat(actualLinea.tipoMaterialId()).isEqualTo(material.getId());
        var lineaGuardada = captorLinea.getValue();
        assertThat(lineaGuardada.getStockMinimo()).isEqualByComparingTo("10.00");
        assertThat(lineaGuardada.getStockMaximo()).isEqualByComparingTo("100.00");
        assertThat(lineaGuardada.getBodega()).isEqualTo(bodega);
        assertThat(lineaGuardada.getTipoMaterial()).isEqualTo(material);
    }

    @Test
    void crearLinea_lineaDuplicada_lanzaLineaDuplicadaException() {
        // Given
        var bodegaId = UUID.randomUUID();
        var tipoMaterialId = UUID.randomUUID();
        var cuerpo = new CuerpoCrearLinea(bodegaId, tipoMaterialId, new BigDecimal("10.00"),
                new BigDecimal("100.00"));
        when(lineaRepository.existsByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)).thenReturn(true);

        // When-Then
        assertThatThrownBy(() -> inventarioService.crearLinea(cuerpo))
                .isInstanceOf(LineaDuplicadaException.class);
        verify(bodegaRepository, never()).findById(any());
    }

    @Test
    void crearLinea_topesInvalidos_lanzaStockInvalidoException() {
        // Given
        var bodegaId = UUID.randomUUID();
        var tipoMaterialId = UUID.randomUUID();
        var cuerpo = new CuerpoCrearLinea(bodegaId, tipoMaterialId, new BigDecimal("100.00"),
                new BigDecimal("10.00"));
        when(lineaRepository.existsByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)).thenReturn(false);

        // When-Then
        assertThatThrownBy(() -> inventarioService.crearLinea(cuerpo))
                .isInstanceOf(StockInvalidoException.class);
        verify(bodegaRepository, never()).findById(any());
    }

    @Test
    void crearLinea_bodegaInexistente_lanzaBodegaNoEncontradaException() {
        // Given
        var bodegaId = UUID.randomUUID();
        var tipoMaterialId = UUID.randomUUID();
        var cuerpo = new CuerpoCrearLinea(bodegaId, tipoMaterialId, new BigDecimal("10.00"),
                new BigDecimal("100.00"));
        when(lineaRepository.existsByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)).thenReturn(false);
        when(bodegaRepository.findById(bodegaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.crearLinea(cuerpo))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    @Test
    void crearLinea_materialInexistente_lanzaMaterialNoEncontradoException() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var tipoMaterialId = UUID.randomUUID();
        var cuerpo = new CuerpoCrearLinea(bodega.getId(), tipoMaterialId, new BigDecimal("10.00"),
                new BigDecimal("100.00"));
        when(lineaRepository.existsByBodegaIdAndTipoMaterialId(bodega.getId(), tipoMaterialId)).thenReturn(false);
        when(bodegaRepository.findById(bodega.getId())).thenReturn(Optional.of(bodega));
        when(materialRepository.findById(tipoMaterialId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.crearLinea(cuerpo))
                .isInstanceOf(MaterialNoEncontradoException.class);
    }

    // ---------- actualizarTopes ----------

    @Test
    void actualizarTopes_topesValidos_actualizaLinea() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("50.00"), new BigDecimal("1.00"),
                new BigDecimal("10.00"));
        var cuerpo = new CuerpoTopes(new BigDecimal("5.00"), new BigDecimal("50.00"));
        when(lineaRepository.findById(linea.getId())).thenReturn(Optional.of(linea));

        // When
        var actualLinea = inventarioService.actualizarTopes(linea.getId(), cuerpo);

        // Then
        assertThat(actualLinea.stockMinimo()).isEqualByComparingTo("5.00");
        assertThat(actualLinea.stockMaximo()).isEqualByComparingTo("50.00");
    }

    @Test
    void actualizarTopes_topesInvalidos_lanzaStockInvalidoException() {
        // Given
        var lineaId = UUID.randomUUID();
        var cuerpo = new CuerpoTopes(new BigDecimal("100.00"), new BigDecimal("10.00"));

        // When-Then
        assertThatThrownBy(() -> inventarioService.actualizarTopes(lineaId, cuerpo))
                .isInstanceOf(StockInvalidoException.class);
        verify(lineaRepository, never()).findById(any());
    }

    @Test
    void actualizarTopes_lineaInexistente_lanzaLineaInventarioNoEncontradaException() {
        // Given
        var lineaId = UUID.randomUUID();
        var cuerpo = new CuerpoTopes(new BigDecimal("5.00"), new BigDecimal("50.00"));
        when(lineaRepository.findById(lineaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.actualizarTopes(lineaId, cuerpo))
                .isInstanceOf(LineaInventarioNoEncontradaException.class);
    }

    // ---------- registrarAjuste ----------

    @Test
    void registrarAjuste_lineaExistente_actualizaStockYRegistraMovimiento() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("50.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"));
        var cuerpo = new CuerpoAjuste(new BigDecimal("80.00"), "Conteo fisico");
        when(lineaRepository.findById(linea.getId())).thenReturn(Optional.of(linea));
        var captorMovimiento = ArgumentCaptor.forClass(MovimientoInventario.class);
        when(movimientoRepository.save(captorMovimiento.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Admin Uno");

            // When
            var actualLinea = inventarioService.registrarAjuste(linea.getId(), cuerpo);

            // Then
            assertThat(actualLinea.stockActual()).isEqualByComparingTo("80.00");
            var movimientoGuardado = captorMovimiento.getValue();
            assertThat(movimientoGuardado.getTipoOperacion()).isEqualTo(TipoOperacion.AJUSTE);
            assertThat(movimientoGuardado.getCantidad()).isEqualByComparingTo("30.00");
            assertThat(movimientoGuardado.getCantidadAnterior()).isEqualByComparingTo("50.00");
            assertThat(movimientoGuardado.getCantidadNueva()).isEqualByComparingTo("80.00");
            assertThat(movimientoGuardado.getReferencia()).isEqualTo("Conteo fisico");
        }
    }

    @Test
    void registrarAjuste_lineaInexistente_lanzaLineaInventarioNoEncontradaException() {
        // Given
        var lineaId = UUID.randomUUID();
        var cuerpo = new CuerpoAjuste(new BigDecimal("80.00"), "Conteo fisico");
        when(lineaRepository.findById(lineaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.registrarAjuste(lineaId, cuerpo))
                .isInstanceOf(LineaInventarioNoEncontradaException.class);
    }

    // ---------- registrarMerma ----------

    @Test
    void registrarMerma_cantidadDentroDeStock_descuentaStockYRegistraMovimiento() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"));
        var cuerpo = new CuerpoMerma(new BigDecimal("20.00"), "Material danado");
        when(lineaRepository.findById(linea.getId())).thenReturn(Optional.of(linea));
        var captorMovimiento = ArgumentCaptor.forClass(MovimientoInventario.class);
        when(movimientoRepository.save(captorMovimiento.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Admin Uno");

            // When
            var actualLinea = inventarioService.registrarMerma(linea.getId(), cuerpo);

            // Then
            assertThat(actualLinea.stockActual()).isEqualByComparingTo("80.00");
            var movimientoGuardado = captorMovimiento.getValue();
            assertThat(movimientoGuardado.getTipoOperacion()).isEqualTo(TipoOperacion.MERMA);
            assertThat(movimientoGuardado.getCantidad()).isEqualByComparingTo("20.00");
            assertThat(movimientoGuardado.getCantidadAnterior()).isEqualByComparingTo("100.00");
            assertThat(movimientoGuardado.getCantidadNueva()).isEqualByComparingTo("80.00");
            assertThat(movimientoGuardado.getReferencia()).isEqualTo("Material danado");
        }
    }

    @Test
    void registrarMerma_cantidadSuperaStock_lanzaStockInvalidoException() {
        // Given
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Sur");
        var material = crearMaterial(UUID.randomUUID(), "PET", "Plasticos", UnidadMedida.KILOGRAMO);
        var linea = crearLinea(UUID.randomUUID(), bodega, material, new BigDecimal("10.00"), BigDecimal.ZERO,
                new BigDecimal("500.00"));
        var cuerpo = new CuerpoMerma(new BigDecimal("50.00"), "Material danado");
        when(lineaRepository.findById(linea.getId())).thenReturn(Optional.of(linea));

        // When-Then
        assertThatThrownBy(() -> inventarioService.registrarMerma(linea.getId(), cuerpo))
                .isInstanceOf(StockInvalidoException.class);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void registrarMerma_lineaInexistente_lanzaLineaInventarioNoEncontradaException() {
        // Given
        var lineaId = UUID.randomUUID();
        var cuerpo = new CuerpoMerma(new BigDecimal("20.00"), "Material danado");
        when(lineaRepository.findById(lineaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> inventarioService.registrarMerma(lineaId, cuerpo))
                .isInstanceOf(LineaInventarioNoEncontradaException.class);
    }

    // ---------- helpers ----------

    private Bodega crearBodega(UUID id, String nombre) {
        return Bodega.builder()
                .id(id)
                .nombre(nombre)
                .build();
    }

    private Material crearMaterial(UUID id, String nombre, String categoriaNombre, UnidadMedida unidadMedida) {
        var categoria = OpcionCatalogo.builder()
                .id(UUID.randomUUID())
                .tipo(TipoOpcionCatalogo.CATEGORIA)
                .codigo("CAT")
                .nombre(categoriaNombre)
                .build();
        return Material.builder()
                .id(id)
                .nombre(nombre)
                .categoria(categoria)
                .unidadMedida(unidadMedida)
                .build();
    }

    private LineaInventario crearLinea(UUID id, Bodega bodega, Material material, BigDecimal stockActual,
            BigDecimal stockMinimo, BigDecimal stockMaximo) {
        return LineaInventario.builder()
                .id(id)
                .bodega(bodega)
                .tipoMaterial(material)
                .stockActual(stockActual)
                .stockMinimo(stockMinimo)
                .stockMaximo(stockMaximo)
                .build();
    }
}
