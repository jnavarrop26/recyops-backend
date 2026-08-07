package com.recyops.api.unit.proveedor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import com.recyops.api.entrega.repository.EntregaRepository;
import com.recyops.api.material.entity.Material;
import com.recyops.api.proveedor.dtos.CuerpoProveedor;
import com.recyops.api.proveedor.dtos.RespuestaEntregaProveedor;
import com.recyops.api.proveedor.dtos.RespuestaProveedor;
import com.recyops.api.proveedor.entity.Proveedor;
import com.recyops.api.proveedor.enums.EstadoProveedor;
import com.recyops.api.proveedor.excepciones.CalificacionInvalidaException;
import com.recyops.api.proveedor.excepciones.ProveedorNoEncontradoException;
import com.recyops.api.proveedor.repository.ProveedorRepository;
import com.recyops.api.proveedor.service.ProveedorServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class ProveedorServiceImplTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private EntregaRepository entregaRepository;

    @InjectMocks
    private ProveedorServiceImpl proveedorService;

    @Test
    void listar_conFiltros_retornaPaginaMapeada() {
        // Given
        var proveedor = Proveedor.builder()
                .id(UUID.randomUUID())
                .nombre("Proveedor Norte")
                .nit("900111000")
                .calificacion(BigDecimal.valueOf(4.0))
                .estado(EstadoProveedor.ACTIVO)
                .build();
        Page<Proveedor> pagina = new PageImpl<>(List.of(proveedor), PageRequest.of(0, 20), 1);
        when(proveedorRepository.buscar(eq(EstadoProveedor.ACTIVO), eq("Norte"), eq(BigDecimal.valueOf(3.0)),
                any(PageRequest.class)))
                .thenReturn(pagina);

        // When
        RespuestaPagina<RespuestaProveedor> actualResultado =
                proveedorService.listar(EstadoProveedor.ACTIVO, "Norte", BigDecimal.valueOf(3.0), 0, 20);

        // Then
        assertThat(actualResultado.content()).hasSize(1);
        assertThat(actualResultado.content().get(0).nombre()).isEqualTo("Proveedor Norte");
    }

    @Test
    void crear_datosValidos_guardaYRetornaProveedor() {
        // Given
        var cuerpo = new CuerpoProveedor("Proveedor Nuevo", "900222333", "Juan", "3011112222",
                "contacto@proveedor.com", "Calle 5");
        var captor = ArgumentCaptor.forClass(Proveedor.class);
        UUID idGenerado = UUID.randomUUID();
        when(proveedorRepository.save(captor.capture())).thenAnswer(invocation -> {
            Proveedor guardado = invocation.getArgument(0);
            guardado.setId(idGenerado);
            return guardado;
        });

        // When
        RespuestaProveedor actualRespuesta = proveedorService.crear(cuerpo);

        // Then
        Proveedor proveedorCapturado = captor.getValue();
        assertThat(proveedorCapturado.getNombre()).isEqualTo("Proveedor Nuevo");
        assertThat(proveedorCapturado.getNit()).isEqualTo("900222333");
        assertThat(actualRespuesta.id()).isEqualTo(idGenerado);
    }

    @Test
    void actualizar_idExistente_actualizaCamposYRetornaProveedor() {
        // Given
        UUID id = UUID.randomUUID();
        var proveedorExistente = Proveedor.builder()
                .id(id)
                .nombre("Nombre Viejo")
                .nit("900000111")
                .calificacion(BigDecimal.ZERO)
                .estado(EstadoProveedor.ACTIVO)
                .build();
        when(proveedorRepository.findById(id)).thenReturn(Optional.of(proveedorExistente));
        var cuerpo = new CuerpoProveedor("Nombre Nuevo", "900999777", "Maria", "3022223333",
                "nueva@proveedor.com", "Calle 6");

        // When
        RespuestaProveedor actualRespuesta = proveedorService.actualizar(id, cuerpo);

        // Then
        assertThat(actualRespuesta.nombre()).isEqualTo("Nombre Nuevo");
        assertThat(actualRespuesta.nit()).isEqualTo("900999777");
    }

    @Test
    void actualizar_idInexistente_lanzaProveedorNoEncontradoException() {
        // Given
        UUID id = UUID.randomUUID();
        when(proveedorRepository.findById(id)).thenReturn(Optional.empty());
        var cuerpo = new CuerpoProveedor("Nombre", "900", "Contacto", "300", "a@b.com", "Calle");

        // When-Then
        assertThatThrownBy(() -> proveedorService.actualizar(id, cuerpo))
                .isInstanceOf(ProveedorNoEncontradoException.class);
    }

    @Test
    void cambiarEstado_idExistente_actualizaEstadoYRetornaProveedor() {
        // Given
        UUID id = UUID.randomUUID();
        var proveedorExistente = Proveedor.builder()
                .id(id)
                .nombre("Proveedor Central")
                .nit("900444555")
                .calificacion(BigDecimal.ZERO)
                .estado(EstadoProveedor.ACTIVO)
                .build();
        when(proveedorRepository.findById(id)).thenReturn(Optional.of(proveedorExistente));

        // When
        RespuestaProveedor actualRespuesta = proveedorService.cambiarEstado(id, EstadoProveedor.BLOQUEADO);

        // Then
        assertThat(actualRespuesta.estado()).isEqualTo(EstadoProveedor.BLOQUEADO);
    }

    @Test
    void calificar_valorValido_actualizaCalificacionYRetornaProveedor() {
        // Given
        UUID id = UUID.randomUUID();
        var proveedorExistente = Proveedor.builder()
                .id(id)
                .nombre("Proveedor Calificado")
                .nit("900666777")
                .calificacion(BigDecimal.ZERO)
                .estado(EstadoProveedor.ACTIVO)
                .build();
        when(proveedorRepository.findById(id)).thenReturn(Optional.of(proveedorExistente));

        // When
        RespuestaProveedor actualRespuesta = proveedorService.calificar(id, 4.5);

        // Then
        assertThat(actualRespuesta.calificacion()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void calificar_valorFueraDeRango_lanzaCalificacionInvalidaException() {
        // Given
        UUID id = UUID.randomUUID();

        // When-Then
        assertThatThrownBy(() -> proveedorService.calificar(id, -1.0))
                .isInstanceOf(CalificacionInvalidaException.class);
    }

    @Test
    void entregas_idExistente_retornaListaDeEntregasDelProveedor() {
        // Given
        UUID id = UUID.randomUUID();
        var proveedorExistente = Proveedor.builder()
                .id(id)
                .nombre("Proveedor con Entregas")
                .nit("900888999")
                .calificacion(BigDecimal.ZERO)
                .estado(EstadoProveedor.ACTIVO)
                .build();
        when(proveedorRepository.findById(id)).thenReturn(Optional.of(proveedorExistente));
        var material = Material.builder().nombre("PET").build();
        var entrega = Entrega.builder()
                .id(UUID.randomUUID())
                .codigo("ENT-000001")
                .tipoMaterial(material)
                .pesoKg(BigDecimal.valueOf(120))
                .estado(EstadoEntrega.RECIBIDA)
                .fechaRecepcion(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
        when(entregaRepository.findByProveedorIdOrderByFechaRecepcionDesc(id)).thenReturn(List.of(entrega));

        // When
        List<RespuestaEntregaProveedor> actualEntregas = proveedorService.entregas(id);

        // Then
        assertThat(actualEntregas).hasSize(1);
        assertThat(actualEntregas.get(0).tipoMaterialNombre()).isEqualTo("PET");
    }
}
