package com.recyops.api.integration.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.enums.TipoOrganizacion;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.integration.ApiIntegrationTest;
import com.recyops.api.tenant.ContextoEmpresa;
import com.recyops.api.tenant.MigradorEsquemas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cubre el mecanismo central de la multi-tenencia: que {@link ResolutorEsquemaEmpresa}
 * + {@link ProveedorConexionesPorEsquema} enruten cada sesion de Hibernate al
 * search_path correcto segun {@link ContextoEmpresa}, y que una empresa jamas
 * pueda leer datos de otra. Cada repository de Spring Data abre su propia
 * transaccion (por eso NO se anota el test con @Transactional): asi cada
 * llamada resuelve el tenant de nuevo, igual que cada peticion HTTP real.
 */
class AislamientoEsquemaEmpresaIT extends ApiIntegrationTest {

    @Autowired
    private MigradorEsquemas migradorEsquemas;

    @Autowired
    private BodegaRepository bodegaRepository;

    @AfterEach
    void limpiarContexto() {
        ContextoEmpresa.limpiar();
    }

    @Test
    void unaEmpresaNoVeLosDatosDeOtra() {
        migradorEsquemas.migrarEsquemaEmpresa("empresa_test_a");
        migradorEsquemas.migrarEsquemaEmpresa("empresa_test_b");

        ContextoEmpresa.establecer("empresa_test_a");
        Bodega bodegaA = bodegaRepository.save(nuevaBodega("Bodega A"));

        ContextoEmpresa.establecer("empresa_test_b");
        Bodega bodegaB = bodegaRepository.save(nuevaBodega("Bodega B"));

        ContextoEmpresa.establecer("empresa_test_a");
        assertThat(bodegaRepository.findAll()).extracting(Bodega::getId).containsExactly(bodegaA.getId());
        assertThat(bodegaRepository.findById(bodegaB.getId())).isEmpty();

        ContextoEmpresa.establecer("empresa_test_b");
        assertThat(bodegaRepository.findAll()).extracting(Bodega::getId).containsExactly(bodegaB.getId());
        assertThat(bodegaRepository.findById(bodegaA.getId())).isEmpty();
    }

    private Bodega nuevaBodega(String nombre) {
        return Bodega.builder()
                .nombre(nombre)
                .direccion("Calle 1 # 2-3")
                .nit("900123456-" + nombre.hashCode())
                .tipoOrganizacion(TipoOrganizacion.PROPIA)
                .build();
    }
}
