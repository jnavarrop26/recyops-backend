package com.recyops.api.unit.convenio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.convenio.dtos.CuerpoConvenio;
import com.recyops.api.convenio.dtos.RespuestaConvenio;
import com.recyops.api.convenio.entity.Convenio;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import com.recyops.api.convenio.excepciones.ConvenioNoEncontradoException;
import com.recyops.api.convenio.excepciones.FechasConvenioInvalidasException;
import com.recyops.api.convenio.repository.ConvenioRepository;
import com.recyops.api.convenio.service.ConvenioServiceImpl;
import com.recyops.api.proveedor.excepciones.ProveedorNoEncontradoException;
import com.recyops.api.proveedor.repository.ProveedorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConvenioServiceImplTest {

    @Mock
    private ConvenioRepository convenioRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @InjectMocks
    private ConvenioServiceImpl convenioService;

    @Test
    void listar_conFiltros_retornaPaginaMapeada() {
        // Given
        var convenio = Convenio.builder()
                .id(UUID.randomUUID())
                .codigo("CNV-000001")
                .nombre("Convenio Norte")
                .tipo(TipoConvenio.COMPRA)
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .estado(EstadoConvenio.ACTIVO)
                .build();
        Page<Convenio> pagina = new PageImpl<>(List.of(convenio), PageRequest.of(0, 20), 1);
        when(convenioRepository.buscar(eq(EstadoConvenio.ACTIVO), eq(TipoConvenio.COMPRA), eq("Norte"),
                any(PageRequest.class)))
                .thenReturn(pagina);

        // When
        RespuestaPagina<RespuestaConvenio> actualResultado =
                convenioService.listar(EstadoConvenio.ACTIVO, TipoConvenio.COMPRA, "Norte", 0, 20);

        // Then
        assertThat(actualResultado.content()).hasSize(1);
        assertThat(actualResultado.content().get(0).nombre()).isEqualTo("Convenio Norte");
    }

    @Test
    void obtener_idExistente_retornaConvenio() {
        // Given
        UUID id = UUID.randomUUID();
        var convenio = Convenio.builder()
                .id(id)
                .codigo("CNV-000002")
                .nombre("Convenio Sur")
                .tipo(TipoConvenio.VENTA)
                .fechaInicio(LocalDate.of(2026, 2, 1))
                .estado(EstadoConvenio.ACTIVO)
                .build();
        when(convenioRepository.findById(id)).thenReturn(Optional.of(convenio));

        // When
        RespuestaConvenio actualRespuesta = convenioService.obtener(id);

        // Then
        assertThat(actualRespuesta.id()).isEqualTo(id);
        assertThat(actualRespuesta.nombre()).isEqualTo("Convenio Sur");
    }

    @Test
    void obtener_idInexistente_lanzaConvenioNoEncontradoException() {
        // Given
        UUID id = UUID.randomUUID();
        when(convenioRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> convenioService.obtener(id))
                .isInstanceOf(ConvenioNoEncontradoException.class);
    }

    @Test
    void crear_datosValidos_generaCodigoYGuardaConvenio() {
        // Given
        when(convenioRepository.siguienteConsecutivo()).thenReturn(7L);
        when(convenioRepository.save(any(Convenio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var cuerpo = new CuerpoConvenio("Convenio Nuevo", TipoConvenio.SERVICIO, null, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.TEN, "Ana", "Detalle");

        // When
        RespuestaConvenio actualRespuesta = convenioService.crear(cuerpo);

        // Then
        assertThat(actualRespuesta.codigo()).isEqualTo("CNV-000007");
        assertThat(actualRespuesta.nombre()).isEqualTo("Convenio Nuevo");
        assertThat(actualRespuesta.tipo()).isEqualTo(TipoConvenio.SERVICIO);
    }

    @Test
    void crear_fechaFinAnteriorAFechaInicio_lanzaFechasConvenioInvalidasException() {
        // Given
        var cuerpo = new CuerpoConvenio("Convenio Invalido", TipoConvenio.COMPRA, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null, null, null);

        // When-Then
        assertThatThrownBy(() -> convenioService.crear(cuerpo))
                .isInstanceOf(FechasConvenioInvalidasException.class);
    }

    @Test
    void crear_proveedorIdInexistente_lanzaProveedorNoEncontradoException() {
        // Given
        UUID proveedorId = UUID.randomUUID();
        when(proveedorRepository.findById(proveedorId)).thenReturn(Optional.empty());
        var cuerpo = new CuerpoConvenio("Convenio X", TipoConvenio.COMPRA, proveedorId, null,
                LocalDate.of(2026, 1, 1), null, null, null, null);

        // When-Then
        assertThatThrownBy(() -> convenioService.crear(cuerpo))
                .isInstanceOf(ProveedorNoEncontradoException.class);
    }

    @Test
    void crear_bodegaIdInexistente_lanzaBodegaNoEncontradaException() {
        // Given
        UUID bodegaId = UUID.randomUUID();
        when(bodegaRepository.findById(bodegaId)).thenReturn(Optional.empty());
        var cuerpo = new CuerpoConvenio("Convenio Y", TipoConvenio.COMPRA, null, bodegaId,
                LocalDate.of(2026, 1, 1), null, null, null, null);

        // When-Then
        assertThatThrownBy(() -> convenioService.crear(cuerpo))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    @Test
    void actualizar_idExistente_actualizaCamposYRetornaConvenio() {
        // Given
        UUID id = UUID.randomUUID();
        var convenioExistente = Convenio.builder()
                .id(id)
                .codigo("CNV-000003")
                .nombre("Nombre Viejo")
                .tipo(TipoConvenio.COMPRA)
                .fechaInicio(LocalDate.of(2025, 1, 1))
                .estado(EstadoConvenio.ACTIVO)
                .build();
        when(convenioRepository.findById(id)).thenReturn(Optional.of(convenioExistente));
        var cuerpo = new CuerpoConvenio("Nombre Nuevo", TipoConvenio.INTERCAMBIO, null, null,
                LocalDate.of(2026, 3, 1), null, BigDecimal.valueOf(500), "Luis", "Nueva descripcion");

        // When
        RespuestaConvenio actualRespuesta = convenioService.actualizar(id, cuerpo);

        // Then
        assertThat(actualRespuesta.nombre()).isEqualTo("Nombre Nuevo");
        assertThat(actualRespuesta.tipo()).isEqualTo(TipoConvenio.INTERCAMBIO);
        assertThat(actualRespuesta.responsable()).isEqualTo("Luis");
    }

    @Test
    void cambiarEstado_idExistente_actualizaEstadoYRetornaConvenio() {
        // Given
        UUID id = UUID.randomUUID();
        var convenioExistente = Convenio.builder()
                .id(id)
                .codigo("CNV-000004")
                .nombre("Convenio Central")
                .tipo(TipoConvenio.COMPRA)
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .estado(EstadoConvenio.ACTIVO)
                .build();
        when(convenioRepository.findById(id)).thenReturn(Optional.of(convenioExistente));

        // When
        RespuestaConvenio actualRespuesta = convenioService.cambiarEstado(id, EstadoConvenio.SUSPENDIDO);

        // Then
        assertThat(actualRespuesta.estado()).isEqualTo(EstadoConvenio.SUSPENDIDO);
    }
}
