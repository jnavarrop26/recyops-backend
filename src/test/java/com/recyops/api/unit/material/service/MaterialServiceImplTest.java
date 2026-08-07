package com.recyops.api.unit.material.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.recyops.api.material.dtos.CuerpoMaterial;
import com.recyops.api.material.entity.Material;
import com.recyops.api.material.entity.OpcionCatalogo;
import com.recyops.api.material.enums.TipoOpcionCatalogo;
import com.recyops.api.material.enums.UnidadEmpaque;
import com.recyops.api.material.enums.UnidadMedida;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.excepciones.OpcionCatalogoNoEncontradaException;
import com.recyops.api.material.repository.MaterialRepository;
import com.recyops.api.material.repository.OpcionCatalogoRepository;
import com.recyops.api.material.service.MaterialServiceImpl;
import java.math.BigDecimal;
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

@ExtendWith(MockitoExtension.class)
class MaterialServiceImplTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private OpcionCatalogoRepository catalogoRepository;

    @InjectMocks
    private MaterialServiceImpl materialService;

    // ---- listar ----

    @Test
    void listar_conFiltros_retornaPaginaDeMateriales() {
        // Given
        var categoriaOpcion = OpcionCatalogo.builder().tipo(TipoOpcionCatalogo.CATEGORIA)
                .codigo("PLASTICO").nombre("Plastico").build();
        var material = Material.builder()
                .id(UUID.randomUUID())
                .nombre("PET transparente")
                .categoria(categoriaOpcion)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .unidadEmpaque(UnidadEmpaque.GRANEL)
                .precioBase(new BigDecimal("1200.00"))
                .factorCalidad(BigDecimal.ONE)
                .build();
        var pagina = new PageImpl<>(List.of(material), PageRequest.of(0, 20), 1);
        when(materialRepository.buscar("PLASTICO", null, null, null, true, PageRequest.of(0, 20)))
                .thenReturn(pagina);

        // When
        var actualPagina = materialService.listar("PLASTICO", null, null, null, true, 0, 20);

        // Then
        assertThat(actualPagina.content()).hasSize(1);
        assertThat(actualPagina.content().get(0).nombre()).isEqualTo("PET transparente");
        assertThat(actualPagina.totalElements()).isEqualTo(1);
    }

    // ---- crear ----

    @Test
    void crear_datosValidosSinClasificadoresOpcionales_creaYRetornaMaterial() {
        // Given
        var cuerpo = new CuerpoMaterial("PET transparente", "PLASTICO", null, null, null,
                UnidadMedida.KILOGRAMO, UnidadEmpaque.GRANEL, new BigDecimal("1200.00"), BigDecimal.ONE, null);
        var categoriaOpcion = OpcionCatalogo.builder().tipo(TipoOpcionCatalogo.CATEGORIA)
                .codigo("PLASTICO").nombre("Plastico").build();
        when(catalogoRepository.findByTipoAndCodigo(TipoOpcionCatalogo.CATEGORIA, "PLASTICO"))
                .thenReturn(Optional.of(categoriaOpcion));
        var captorMaterial = ArgumentCaptor.forClass(Material.class);
        when(materialRepository.save(captorMaterial.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        var actualRespuesta = materialService.crear(cuerpo);

        // Then
        assertThat(actualRespuesta.nombre()).isEqualTo("PET transparente");
        assertThat(actualRespuesta.categoriaCodigo()).isEqualTo("PLASTICO");
        var materialGuardado = captorMaterial.getValue();
        assertThat(materialGuardado.getNombre()).isEqualTo("PET transparente");
        assertThat(materialGuardado.getCategoria()).isEqualTo(categoriaOpcion);
        assertThat(materialGuardado.getSubcategoria()).isNull();
        assertThat(materialGuardado.getResina()).isNull();
        assertThat(materialGuardado.getColor()).isNull();
    }

    @Test
    void crear_categoriaInexistente_lanzaOpcionCatalogoNoEncontradaException() {
        // Given
        var cuerpo = new CuerpoMaterial("PET transparente", "DESCONOCIDO", null, null, null,
                UnidadMedida.KILOGRAMO, UnidadEmpaque.GRANEL, new BigDecimal("1200.00"), BigDecimal.ONE, null);
        when(catalogoRepository.findByTipoAndCodigo(TipoOpcionCatalogo.CATEGORIA, "DESCONOCIDO"))
                .thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> materialService.crear(cuerpo))
                .isInstanceOf(OpcionCatalogoNoEncontradaException.class);
    }

    // ---- actualizar ----

    @Test
    void actualizar_materialExistente_actualizaMaterialYRetornaRespuesta() {
        // Given
        var id = UUID.randomUUID();
        var materialExistente = Material.builder().id(id).nombre("Nombre viejo")
                .unidadMedida(UnidadMedida.KILOGRAMO).unidadEmpaque(UnidadEmpaque.GRANEL)
                .precioBase(BigDecimal.TEN).factorCalidad(BigDecimal.ONE).build();
        when(materialRepository.findById(id)).thenReturn(Optional.of(materialExistente));
        var categoriaOpcion = OpcionCatalogo.builder().tipo(TipoOpcionCatalogo.CATEGORIA)
                .codigo("PLASTICO").nombre("Plastico").build();
        when(catalogoRepository.findByTipoAndCodigo(TipoOpcionCatalogo.CATEGORIA, "PLASTICO"))
                .thenReturn(Optional.of(categoriaOpcion));
        var cuerpo = new CuerpoMaterial("PET verde", "PLASTICO", null, null, null,
                UnidadMedida.TONELADA, UnidadEmpaque.PACA, new BigDecimal("1500.00"), new BigDecimal("0.9"), null);

        // When
        var actualRespuesta = materialService.actualizar(id, cuerpo);

        // Then
        assertThat(actualRespuesta.nombre()).isEqualTo("PET verde");
        assertThat(actualRespuesta.unidadMedida()).isEqualTo("TONELADA");
        assertThat(actualRespuesta.precioBase()).isEqualByComparingTo("1500.00");
    }

    @Test
    void actualizar_materialInexistente_lanzaMaterialNoEncontradoException() {
        // Given
        var id = UUID.randomUUID();
        when(materialRepository.findById(id)).thenReturn(Optional.empty());
        var cuerpo = new CuerpoMaterial("PET verde", "PLASTICO", null, null, null,
                UnidadMedida.TONELADA, UnidadEmpaque.PACA, new BigDecimal("1500.00"), new BigDecimal("0.9"), null);

        // When-Then
        assertThatThrownBy(() -> materialService.actualizar(id, cuerpo))
                .isInstanceOf(MaterialNoEncontradoException.class);
    }

    // ---- cambiarActivo ----

    @Test
    void cambiarActivo_materialExistente_actualizaEstadoActivo() {
        // Given
        var id = UUID.randomUUID();
        var material = Material.builder().id(id).nombre("PET")
                .unidadMedida(UnidadMedida.KILOGRAMO).unidadEmpaque(UnidadEmpaque.GRANEL)
                .precioBase(BigDecimal.TEN).factorCalidad(BigDecimal.ONE).activo(true).build();
        when(materialRepository.findById(id)).thenReturn(Optional.of(material));

        // When
        var actualRespuesta = materialService.cambiarActivo(id, false);

        // Then
        assertThat(actualRespuesta.activo()).isFalse();
    }

    @Test
    void cambiarActivo_materialInexistente_lanzaMaterialNoEncontradoException() {
        // Given
        var id = UUID.randomUUID();
        when(materialRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> materialService.cambiarActivo(id, false))
                .isInstanceOf(MaterialNoEncontradoException.class);
    }

    // ---- catalogos ----

    @Test
    void listarCategorias_existenCategorias_retornaOpcionesMapeadas() {
        // Given
        var opcion = OpcionCatalogo.builder().codigo("PLASTICO").nombre("Plastico").esTermoplastico(true).build();
        when(catalogoRepository.findByTipoOrderByNombre(TipoOpcionCatalogo.CATEGORIA)).thenReturn(List.of(opcion));

        // When
        var actualLista = materialService.listarCategorias();

        // Then
        assertThat(actualLista).hasSize(1);
        assertThat(actualLista.get(0).codigo()).isEqualTo("PLASTICO");
        assertThat(actualLista.get(0).nombre()).isEqualTo("Plastico");
    }

    @Test
    void listarSubcategorias_categoriaDada_retornaOpcionesDeEsaCategoria() {
        // Given
        var opcion = OpcionCatalogo.builder().codigo("PET").nombre("PET").build();
        when(catalogoRepository.findByTipoAndCategoriaPadreCodigoOrderByNombre(
                TipoOpcionCatalogo.SUBCATEGORIA, "PLASTICO")).thenReturn(List.of(opcion));

        // When
        var actualLista = materialService.listarSubcategorias("PLASTICO");

        // Then
        assertThat(actualLista).hasSize(1);
        assertThat(actualLista.get(0).codigo()).isEqualTo("PET");
    }

    @Test
    void listarResinas_existenResinas_retornaOpcionesMapeadas() {
        // Given
        var opcion = OpcionCatalogo.builder().codigo("PET").nombre("Polietileno tereftalato").build();
        when(catalogoRepository.findByTipoOrderByNombre(TipoOpcionCatalogo.RESINA)).thenReturn(List.of(opcion));

        // When
        var actualLista = materialService.listarResinas();

        // Then
        assertThat(actualLista).hasSize(1);
        assertThat(actualLista.get(0).codigo()).isEqualTo("PET");
    }

    @Test
    void listarColores_existenColores_retornaOpcionesMapeadas() {
        // Given
        var opcion = OpcionCatalogo.builder().codigo("TRANSPARENTE").nombre("Transparente").build();
        when(catalogoRepository.findByTipoOrderByNombre(TipoOpcionCatalogo.COLOR)).thenReturn(List.of(opcion));

        // When
        var actualLista = materialService.listarColores();

        // Then
        assertThat(actualLista).hasSize(1);
        assertThat(actualLista.get(0).codigo()).isEqualTo("TRANSPARENTE");
    }
}
