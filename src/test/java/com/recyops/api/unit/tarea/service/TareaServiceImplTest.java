package com.recyops.api.unit.tarea.service;

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
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.tarea.dtos.CuerpoAvance;
import com.recyops.api.tarea.dtos.CuerpoTarea;
import com.recyops.api.tarea.dtos.RespuestaTarea;
import com.recyops.api.tarea.entity.AvanceTarea;
import com.recyops.api.tarea.entity.Tarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import com.recyops.api.tarea.enums.PrioridadTarea;
import com.recyops.api.tarea.excepciones.TareaAjenaException;
import com.recyops.api.tarea.excepciones.TareaNoEncontradaException;
import com.recyops.api.tarea.excepciones.TransicionTareaInvalidaException;
import com.recyops.api.tarea.repository.AvanceTareaRepository;
import com.recyops.api.tarea.repository.TareaRepository;
import com.recyops.api.tarea.service.TareaServiceImpl;
import com.recyops.api.usuario.entity.Usuario;
import com.recyops.api.usuario.excepciones.UsuarioNoEncontradoException;
import com.recyops.api.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class TareaServiceImplTest {

    @Mock
    private TareaRepository tareaRepository;

    @Mock
    private AvanceTareaRepository avanceRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @InjectMocks
    private TareaServiceImpl tareaService;

    // ---------- listar ----------

    @Test
    void listar_conResultados_devuelvePaginaMapeada() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), null, "Maria Lopez");
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Norte");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.ALTA, null);
        tarea.setBodega(bodega);
        Page<Tarea> pagina = new PageImpl<>(List.of(tarea), PageRequest.of(0, 10), 1);
        when(tareaRepository.buscar(eq(EstadoTarea.PENDIENTE), eq(asignado.getId()), eq(bodega.getId()),
                any(Pageable.class))).thenReturn(pagina);

        // When
        var actualPagina = tareaService.listar(EstadoTarea.PENDIENTE, asignado.getId(), bodega.getId(), 0, 10);

        // Then
        assertThat(actualPagina.content()).hasSize(1);
        assertThat(actualPagina.content().get(0).titulo()).isEqualTo(tarea.getTitulo());
        assertThat(actualPagina.totalElements()).isEqualTo(1);
        assertThat(actualPagina.totalPages()).isEqualTo(1);
        assertThat(actualPagina.number()).isEqualTo(0);
        assertThat(actualPagina.size()).isEqualTo(10);
    }

    @Test
    void listar_paginaYTamanoDados_solicitaPageRequestCorrecto() {
        // Given
        Page<Tarea> paginaVacia = Page.empty(PageRequest.of(2, 5));
        var captorPageable = ArgumentCaptor.forClass(Pageable.class);
        when(tareaRepository.buscar(any(), any(), any(), captorPageable.capture())).thenReturn(paginaVacia);

        // When
        tareaService.listar(null, null, null, 2, 5);

        // Then
        assertThat(captorPageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captorPageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void listar_sinResultados_devuelvePaginaVacia() {
        // Given
        Page<Tarea> paginaVacia = Page.empty(PageRequest.of(0, 10));
        when(tareaRepository.buscar(any(), any(), any(), any(Pageable.class))).thenReturn(paginaVacia);

        // When
        var actualPagina = tareaService.listar(null, null, null, 0, 10);

        // Then
        assertThat(actualPagina.content()).isEmpty();
        assertThat(actualPagina.totalElements()).isZero();
    }

    // ---------- misTareas ----------

    @Test
    void misTareas_sinUsuarioAutenticado_devuelveListaVacia() {
        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            // Given
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(null);

            // When
            var actualTareas = tareaService.misTareas();

            // Then
            assertThat(actualTareas).isEmpty();
        }
    }

    @Test
    void misTareas_usuarioNoEncontrado_devuelveListaVacia() {
        // Given
        var supabaseId = UUID.randomUUID();
        when(usuarioRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.empty());

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When
            var actualTareas = tareaService.misTareas();

            // Then
            assertThat(actualTareas).isEmpty();
        }
    }

    @Test
    void misTareas_prioridadesDistintas_ordenaAltaPrimero() {
        // Given
        var supabaseId = UUID.randomUUID();
        var usuario = crearUsuario(UUID.randomUUID(), supabaseId, "Juan Perez");
        var tareaBaja = crearTarea(UUID.randomUUID(), usuario, EstadoTarea.PENDIENTE, PrioridadTarea.BAJA, null);
        var tareaAlta = crearTarea(UUID.randomUUID(), usuario, EstadoTarea.PENDIENTE, PrioridadTarea.ALTA, null);
        when(usuarioRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(usuario));
        when(tareaRepository.findByAsignadoIdOrderByFechaCreacionDesc(usuario.getId()))
                .thenReturn(List.of(tareaBaja, tareaAlta));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When
            var actualTareas = tareaService.misTareas();

            // Then
            assertThat(actualTareas).extracting(RespuestaTarea::prioridad)
                    .containsExactly(PrioridadTarea.ALTA, PrioridadTarea.BAJA);
        }
    }

    @Test
    void misTareas_mismaPrioridadFechaLimiteDistinta_ordenaPorFechaMasProxima() {
        // Given
        var supabaseId = UUID.randomUUID();
        var usuario = crearUsuario(UUID.randomUUID(), supabaseId, "Juan Perez");
        var tareaSinFecha = crearTarea(UUID.randomUUID(), usuario, EstadoTarea.PENDIENTE, PrioridadTarea.ALTA, null);
        var tareaConFecha = crearTarea(UUID.randomUUID(), usuario, EstadoTarea.PENDIENTE, PrioridadTarea.ALTA,
                LocalDate.of(2026, 3, 1));
        when(usuarioRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(usuario));
        when(tareaRepository.findByAsignadoIdOrderByFechaCreacionDesc(usuario.getId()))
                .thenReturn(List.of(tareaSinFecha, tareaConFecha));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When
            var actualTareas = tareaService.misTareas();

            // Then
            assertThat(actualTareas).extracting(RespuestaTarea::id)
                    .containsExactly(tareaConFecha.getId(), tareaSinFecha.getId());
        }
    }

    // ---------- crear ----------

    @Test
    void crear_conBodega_guardaTareaConBodegaAsignada() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), null, "Maria Lopez");
        var bodega = crearBodega(UUID.randomUUID(), "Bodega Norte");
        var cuerpo = new CuerpoTarea("Recoger PET", "Descripcion", asignado.getId(), bodega.getId(),
                PrioridadTarea.ALTA, LocalDate.of(2026, 3, 1));
        when(usuarioRepository.findById(asignado.getId())).thenReturn(Optional.of(asignado));
        when(bodegaRepository.findById(bodega.getId())).thenReturn(Optional.of(bodega));
        var captorTarea = ArgumentCaptor.forClass(Tarea.class);
        when(tareaRepository.save(captorTarea.capture())).thenAnswer(invocacion -> invocacion.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Admin Uno");

            // When
            var actualTarea = tareaService.crear(cuerpo);

            // Then
            assertThat(actualTarea.titulo()).isEqualTo("Recoger PET");
            assertThat(actualTarea.bodegaId()).isEqualTo(bodega.getId());
            var tareaGuardada = captorTarea.getValue();
            assertThat(tareaGuardada.getTitulo()).isEqualTo("Recoger PET");
            assertThat(tareaGuardada.getDescripcion()).isEqualTo("Descripcion");
            assertThat(tareaGuardada.getAsignado()).isEqualTo(asignado);
            assertThat(tareaGuardada.getBodega()).isEqualTo(bodega);
            assertThat(tareaGuardada.getPrioridad()).isEqualTo(PrioridadTarea.ALTA);
            assertThat(tareaGuardada.getFechaLimite()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(tareaGuardada.getCreadoPorNombre()).isEqualTo("Admin Uno");
        }
    }

    @Test
    void crear_sinBodega_guardaTareaSinBodega() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), null, "Maria Lopez");
        var cuerpo = new CuerpoTarea("Recoger PET", "Descripcion", asignado.getId(), null,
                PrioridadTarea.MEDIA, null);
        when(usuarioRepository.findById(asignado.getId())).thenReturn(Optional.of(asignado));
        var captorTarea = ArgumentCaptor.forClass(Tarea.class);
        when(tareaRepository.save(captorTarea.capture())).thenAnswer(invocacion -> invocacion.getArgument(0));

        // When
        tareaService.crear(cuerpo);

        // Then
        assertThat(captorTarea.getValue().getBodega()).isNull();
        verify(bodegaRepository, never()).findById(any());
    }

    @Test
    void crear_asignadoInexistente_lanzaUsuarioNoEncontradoException() {
        // Given
        var asignadoId = UUID.randomUUID();
        var cuerpo = new CuerpoTarea("Recoger PET", "Descripcion", asignadoId, null, PrioridadTarea.MEDIA, null);
        when(usuarioRepository.findById(asignadoId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> tareaService.crear(cuerpo))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void crear_bodegaInexistente_lanzaBodegaNoEncontradaException() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), null, "Maria Lopez");
        var bodegaId = UUID.randomUUID();
        var cuerpo = new CuerpoTarea("Recoger PET", "Descripcion", asignado.getId(), bodegaId,
                PrioridadTarea.MEDIA, null);
        when(usuarioRepository.findById(asignado.getId())).thenReturn(Optional.of(asignado));
        when(bodegaRepository.findById(bodegaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> tareaService.crear(cuerpo))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    // ---------- editar ----------

    @Test
    void editar_cuerpoValido_actualizaCampos() {
        // Given
        var asignadoOriginal = crearUsuario(UUID.randomUUID(), null, "Original");
        var asignadoNuevo = crearUsuario(UUID.randomUUID(), null, "Nuevo Asignado");
        var tarea = crearTarea(UUID.randomUUID(), asignadoOriginal, EstadoTarea.PENDIENTE, PrioridadTarea.BAJA,
                null);
        var cuerpo = new CuerpoTarea("Nuevo titulo", "Nueva descripcion", asignadoNuevo.getId(), null,
                PrioridadTarea.ALTA, LocalDate.of(2026, 5, 1));
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findById(asignadoNuevo.getId())).thenReturn(Optional.of(asignadoNuevo));

        // When
        var actualTarea = tareaService.editar(tarea.getId(), cuerpo);

        // Then
        assertThat(actualTarea.titulo()).isEqualTo("Nuevo titulo");
        assertThat(actualTarea.asignadoId()).isEqualTo(asignadoNuevo.getId());
        assertThat(actualTarea.prioridad()).isEqualTo(PrioridadTarea.ALTA);
        assertThat(actualTarea.fechaLimite()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void editar_tareaNoEncontrada_lanzaTareaNoEncontradaException() {
        // Given
        var tareaId = UUID.randomUUID();
        var cuerpo = new CuerpoTarea("Titulo", "Descripcion", UUID.randomUUID(), null, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tareaId)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> tareaService.editar(tareaId, cuerpo))
                .isInstanceOf(TareaNoEncontradaException.class);
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiarEstado_comoAdmin_actualizaEstadoSinFechaCompletada() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), UUID.randomUUID(), "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(true);

            // When
            var actualTarea = tareaService.cambiarEstado(tarea.getId(), EstadoTarea.REVISION);

            // Then
            assertThat(actualTarea.estado()).isEqualTo(EstadoTarea.REVISION);
            assertThat(actualTarea.fechaCompletada()).isNull();
        }
    }

    @Test
    void cambiarEstado_comoAdminACompletada_estableceFechaCompletada() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), UUID.randomUUID(), "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.EN_PROGRESO, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(true);

            // When
            var actualTarea = tareaService.cambiarEstado(tarea.getId(), EstadoTarea.COMPLETADA);

            // Then
            assertThat(actualTarea.estado()).isEqualTo(EstadoTarea.COMPLETADA);
            assertThat(actualTarea.fechaCompletada()).isNotNull();
        }
    }

    @Test
    void cambiarEstado_comoOperarioTareaAjena_lanzaTareaAjenaException() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), UUID.randomUUID(), "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(UUID.randomUUID());

            // When-Then
            assertThatThrownBy(() -> tareaService.cambiarEstado(tarea.getId(), EstadoTarea.EN_PROGRESO))
                    .isInstanceOf(TareaAjenaException.class);
        }
    }

    @Test
    void cambiarEstado_comoOperarioTransicionValida_actualizaEstado() {
        // Given
        var supabaseId = UUID.randomUUID();
        var asignado = crearUsuario(UUID.randomUUID(), supabaseId, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When
            var actualTarea = tareaService.cambiarEstado(tarea.getId(), EstadoTarea.EN_PROGRESO);

            // Then
            assertThat(actualTarea.estado()).isEqualTo(EstadoTarea.EN_PROGRESO);
        }
    }

    @Test
    void cambiarEstado_comoOperarioTransicionInvalida_lanzaTransicionTareaInvalidaException() {
        // Given
        var supabaseId = UUID.randomUUID();
        var asignado = crearUsuario(UUID.randomUUID(), supabaseId, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When-Then
            assertThatThrownBy(() -> tareaService.cambiarEstado(tarea.getId(), EstadoTarea.COMPLETADA))
                    .isInstanceOf(TransicionTareaInvalidaException.class);
        }
    }

    @Test
    void cambiarEstado_comoOperarioIntentaMoverARevision_lanzaTransicionTareaInvalidaException() {
        // Given
        var supabaseId = UUID.randomUUID();
        var asignado = crearUsuario(UUID.randomUUID(), supabaseId, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.COMPLETADA, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When-Then
            assertThatThrownBy(() -> tareaService.cambiarEstado(tarea.getId(), EstadoTarea.REVISION))
                    .isInstanceOf(TransicionTareaInvalidaException.class);
        }
    }

    // ---------- listarAvances ----------

    @Test
    void listarAvances_comoAdmin_devuelveAvancesOrdenados() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), UUID.randomUUID(), "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.EN_PROGRESO, PrioridadTarea.MEDIA, null);
        var avance = AvanceTarea.builder()
                .id(UUID.randomUUID())
                .tarea(tarea)
                .cantidad(BigDecimal.TEN)
                .descripcion("Se cargo el camion")
                .usuarioNombre("Operario")
                .build();
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));
        when(avanceRepository.findByTareaIdOrderByFechaRegistroAsc(tarea.getId())).thenReturn(List.of(avance));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(true);

            // When
            var actualAvances = tareaService.listarAvances(tarea.getId());

            // Then
            assertThat(actualAvances).hasSize(1);
            assertThat(actualAvances.get(0).descripcion()).isEqualTo("Se cargo el camion");
        }
    }

    @Test
    void listarAvances_comoOperarioTareaPropia_devuelveAvances() {
        // Given
        var supabaseId = UUID.randomUUID();
        var asignado = crearUsuario(UUID.randomUUID(), supabaseId, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.EN_PROGRESO, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));
        when(avanceRepository.findByTareaIdOrderByFechaRegistroAsc(tarea.getId())).thenReturn(List.of());

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When
            var actualAvances = tareaService.listarAvances(tarea.getId());

            // Then
            assertThat(actualAvances).isEmpty();
        }
    }

    @Test
    void listarAvances_comoOperarioTareaAjena_lanzaTareaAjenaException() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), UUID.randomUUID(), "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.EN_PROGRESO, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(UUID.randomUUID());

            // When-Then
            assertThatThrownBy(() -> tareaService.listarAvances(tarea.getId()))
                    .isInstanceOf(TareaAjenaException.class);
        }
    }

    // ---------- agregarAvance ----------

    @Test
    void agregarAvance_comoAdmin_guardaAvanceSinValidarEstado() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), UUID.randomUUID(), "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.MEDIA, null);
        var cuerpo = new CuerpoAvance(BigDecimal.TEN, "  Se cargo el camion  ");
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));
        var captorAvance = ArgumentCaptor.forClass(AvanceTarea.class);
        when(avanceRepository.save(captorAvance.capture())).thenAnswer(invocacion -> invocacion.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(true);
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Admin Uno");

            // When
            tareaService.agregarAvance(tarea.getId(), cuerpo);

            // Then
            var avanceGuardado = captorAvance.getValue();
            assertThat(avanceGuardado.getDescripcion()).isEqualTo("Se cargo el camion");
            assertThat(avanceGuardado.getCantidad()).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(avanceGuardado.getUsuarioNombre()).isEqualTo("Admin Uno");
            assertThat(avanceGuardado.getTarea()).isEqualTo(tarea);
        }
    }

    @Test
    void agregarAvance_comoOperarioTareaEnProgreso_guardaAvance() {
        // Given
        var supabaseId = UUID.randomUUID();
        var asignado = crearUsuario(UUID.randomUUID(), supabaseId, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.EN_PROGRESO, PrioridadTarea.MEDIA, null);
        var cuerpo = new CuerpoAvance(BigDecimal.ONE, "Avance parcial");
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));
        when(avanceRepository.save(any(AvanceTarea.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);
            mockedEstatico.when(UsuarioAutenticado::nombreCompleto).thenReturn("Operario");

            // When
            var actualAvance = tareaService.agregarAvance(tarea.getId(), cuerpo);

            // Then
            assertThat(actualAvance.descripcion()).isEqualTo("Avance parcial");
        }
    }

    @Test
    void agregarAvance_comoOperarioTareaNoEnProgreso_lanzaReglaNegocioException() {
        // Given
        var supabaseId = UUID.randomUUID();
        var asignado = crearUsuario(UUID.randomUUID(), supabaseId, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.MEDIA, null);
        var cuerpo = new CuerpoAvance(BigDecimal.ONE, "Avance parcial");
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(supabaseId);

            // When-Then
            assertThatThrownBy(() -> tareaService.agregarAvance(tarea.getId(), cuerpo))
                    .isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Test
    void agregarAvance_comoOperarioTareaAjena_lanzaTareaAjenaException() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), UUID.randomUUID(), "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.EN_PROGRESO, PrioridadTarea.MEDIA, null);
        var cuerpo = new CuerpoAvance(BigDecimal.ONE, "Avance parcial");
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        try (MockedStatic<UsuarioAutenticado> mockedEstatico = mockStatic(UsuarioAutenticado.class)) {
            mockedEstatico.when(UsuarioAutenticado::esAdmin).thenReturn(false);
            mockedEstatico.when(UsuarioAutenticado::supabaseId).thenReturn(UUID.randomUUID());

            // When-Then
            assertThatThrownBy(() -> tareaService.agregarAvance(tarea.getId(), cuerpo))
                    .isInstanceOf(TareaAjenaException.class);
        }
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_tareaEnRevision_eliminaTareaYAvances() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), null, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.REVISION, PrioridadTarea.MEDIA, null);
        var avances = List.of(AvanceTarea.builder().id(UUID.randomUUID()).tarea(tarea).descripcion("Avance")
                .usuarioNombre("Operario").build());
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));
        when(avanceRepository.findByTareaIdOrderByFechaRegistroAsc(tarea.getId())).thenReturn(avances);

        // When
        tareaService.eliminar(tarea.getId());

        // Then
        verify(avanceRepository).deleteAll(avances);
        verify(tareaRepository).delete(tarea);
    }

    @Test
    void eliminar_tareaNoEnRevision_lanzaReglaNegocioException() {
        // Given
        var asignado = crearUsuario(UUID.randomUUID(), null, "Operario");
        var tarea = crearTarea(UUID.randomUUID(), asignado, EstadoTarea.PENDIENTE, PrioridadTarea.MEDIA, null);
        when(tareaRepository.findById(tarea.getId())).thenReturn(Optional.of(tarea));

        // When-Then
        assertThatThrownBy(() -> tareaService.eliminar(tarea.getId()))
                .isInstanceOf(ReglaNegocioException.class);
        verify(tareaRepository, never()).delete(any());
    }

    // ---------- helpers ----------

    private Usuario crearUsuario(UUID id, UUID supabaseId, String nombreCompleto) {
        return Usuario.builder()
                .id(id)
                .supabaseId(supabaseId)
                .nombreCompleto(nombreCompleto)
                .username("usuario-test-" + id)
                .email("usuario-test@test.com")
                .build();
    }

    private Bodega crearBodega(UUID id, String nombre) {
        return Bodega.builder()
                .id(id)
                .nombre(nombre)
                .build();
    }

    private Tarea crearTarea(UUID id, Usuario asignado, EstadoTarea estado, PrioridadTarea prioridad,
            LocalDate fechaLimite) {
        return Tarea.builder()
                .id(id)
                .titulo("Tarea de prueba")
                .descripcion("Descripcion de prueba")
                .asignado(asignado)
                .prioridad(prioridad)
                .estado(estado)
                .fechaLimite(fechaLimite)
                .creadoPorNombre("Admin")
                .build();
    }
}
