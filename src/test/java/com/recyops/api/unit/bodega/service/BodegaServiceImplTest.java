package com.recyops.api.unit.bodega.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.recyops.api.bodega.dtos.CuerpoBodega;
import com.recyops.api.bodega.dtos.RespuestaBodega;
import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.enums.TipoOrganizacion;
import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.bodega.service.BodegaServiceImpl;
import com.recyops.api.comun.dtos.RespuestaPagina;
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

@ExtendWith(MockitoExtension.class)
class BodegaServiceImplTest {

    @Mock
    private BodegaRepository bodegaRepository;

    @InjectMocks
    private BodegaServiceImpl bodegaService;

    @Test
    void listar_conFiltros_retornaPaginaMapeada() {
        // Given
        var bodega = Bodega.builder()
                .id(UUID.randomUUID())
                .nombre("Bodega Norte")
                .direccion("Calle 1")
                .nit("900123456")
                .tipoOrganizacion(TipoOrganizacion.PROPIA)
                .estado(EstadoBodega.ACTIVA)
                .build();
        Page<Bodega> pagina = new PageImpl<>(List.of(bodega), PageRequest.of(0, 20), 1);
        when(bodegaRepository.buscar(eq(EstadoBodega.ACTIVA), eq(TipoOrganizacion.PROPIA), eq("Norte"),
                any(PageRequest.class)))
                .thenReturn(pagina);

        // When
        RespuestaPagina<RespuestaBodega> actualResultado =
                bodegaService.listar(EstadoBodega.ACTIVA, TipoOrganizacion.PROPIA, "Norte", 0, 20);

        // Then
        assertThat(actualResultado.content()).hasSize(1);
        assertThat(actualResultado.content().get(0).nombre()).isEqualTo("Bodega Norte");
        assertThat(actualResultado.totalElements()).isEqualTo(1);
    }

    @Test
    void obtener_idExistente_retornaBodega() {
        // Given
        UUID id = UUID.randomUUID();
        var bodega = Bodega.builder()
                .id(id)
                .nombre("Bodega Sur")
                .direccion("Calle 2")
                .nit("900654321")
                .tipoOrganizacion(TipoOrganizacion.ALIADA)
                .estado(EstadoBodega.ACTIVA)
                .build();
        when(bodegaRepository.findById(id)).thenReturn(Optional.of(bodega));

        // When
        RespuestaBodega actualRespuesta = bodegaService.obtener(id);

        // Then
        assertThat(actualRespuesta.id()).isEqualTo(id);
        assertThat(actualRespuesta.nombre()).isEqualTo("Bodega Sur");
    }

    @Test
    void obtener_idInexistente_lanzaBodegaNoEncontradaException() {
        // Given
        UUID id = UUID.randomUUID();
        when(bodegaRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> bodegaService.obtener(id))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    @Test
    void crear_datosValidos_guardaYRetornaBodega() {
        // Given
        var cuerpo = new CuerpoBodega("Bodega Nueva", "Calle 3", "3001234567",
                "contacto@bodega.com", "900111222", 4.6, -74.1, TipoOrganizacion.TERCERIZADA);
        var captor = ArgumentCaptor.forClass(Bodega.class);
        UUID idGenerado = UUID.randomUUID();
        when(bodegaRepository.save(captor.capture())).thenAnswer(invocation -> {
            Bodega guardada = invocation.getArgument(0);
            guardada.setId(idGenerado);
            return guardada;
        });

        // When
        RespuestaBodega actualRespuesta = bodegaService.crear(cuerpo);

        // Then
        Bodega bodegaCapturada = captor.getValue();
        assertThat(bodegaCapturada.getNombre()).isEqualTo("Bodega Nueva");
        assertThat(bodegaCapturada.getTipoOrganizacion()).isEqualTo(TipoOrganizacion.TERCERIZADA);
        assertThat(actualRespuesta.id()).isEqualTo(idGenerado);
        assertThat(actualRespuesta.nombre()).isEqualTo("Bodega Nueva");
    }

    @Test
    void actualizar_idExistente_actualizaCamposYRetornaBodega() {
        // Given
        UUID id = UUID.randomUUID();
        var bodegaExistente = Bodega.builder()
                .id(id)
                .nombre("Nombre Viejo")
                .direccion("Direccion Vieja")
                .nit("900000000")
                .tipoOrganizacion(TipoOrganizacion.PROPIA)
                .estado(EstadoBodega.ACTIVA)
                .build();
        when(bodegaRepository.findById(id)).thenReturn(Optional.of(bodegaExistente));
        var cuerpo = new CuerpoBodega("Nombre Nuevo", "Direccion Nueva", "3009876543",
                "nueva@bodega.com", "900999888", 4.1, -74.2, TipoOrganizacion.ALIADA);

        // When
        RespuestaBodega actualRespuesta = bodegaService.actualizar(id, cuerpo);

        // Then
        assertThat(actualRespuesta.nombre()).isEqualTo("Nombre Nuevo");
        assertThat(actualRespuesta.direccion()).isEqualTo("Direccion Nueva");
        assertThat(actualRespuesta.tipoOrganizacion()).isEqualTo("ALIADA");
    }

    @Test
    void cambiarEstado_idExistente_actualizaEstadoYRetornaBodega() {
        // Given
        UUID id = UUID.randomUUID();
        var bodegaExistente = Bodega.builder()
                .id(id)
                .nombre("Bodega Central")
                .direccion("Calle 4")
                .nit("900333444")
                .tipoOrganizacion(TipoOrganizacion.PROPIA)
                .estado(EstadoBodega.ACTIVA)
                .build();
        when(bodegaRepository.findById(id)).thenReturn(Optional.of(bodegaExistente));

        // When
        RespuestaBodega actualRespuesta = bodegaService.cambiarEstado(id, EstadoBodega.MANTENIMIENTO);

        // Then
        assertThat(actualRespuesta.estado()).isEqualTo(EstadoBodega.MANTENIMIENTO);
    }
}
