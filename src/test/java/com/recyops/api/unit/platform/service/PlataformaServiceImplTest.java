package com.recyops.api.unit.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recyops.api.platform.dtos.CuerpoNuevaEmpresa;
import com.recyops.api.platform.dtos.RespuestaEmpresaCreada;
import com.recyops.api.platform.excepciones.EmpresaYaExisteException;
import com.recyops.api.platform.excepciones.SchemaInvalidoException;
import com.recyops.api.platform.service.PlataformaServiceImpl;
import com.recyops.api.tenant.MigradorEsquemas;
import com.recyops.api.usuario.service.ClienteSupabaseAdmin;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class PlataformaServiceImplTest {

    private static final String SQL_NIT = "SELECT COUNT(*) FROM public.empresas WHERE nit = ?";
    private static final String SQL_SCHEMA = "SELECT COUNT(*) FROM public.empresas WHERE schema_name = ?";

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbc;

    @Mock
    private ClienteSupabaseAdmin supabaseAdmin;

    @Mock
    private MigradorEsquemas migradorEsquemas;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private PlataformaServiceImpl plataformaService;

    @Test
    void provisionarEmpresa_schemaInvalido_lanzaSchemaInvalidoException() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("1-esquema-invalido", "PasswordAdmin123");

        // When-Then
        assertThatThrownBy(() -> plataformaService.provisionarEmpresa(cuerpo))
                .isInstanceOf(SchemaInvalidoException.class);
    }

    @Test
    void provisionarEmpresa_nitDuplicado_lanzaEmpresaYaExisteException() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("empresa_test", "PasswordAdmin123");
        when(jdbc.queryForObject(eq(SQL_NIT), eq(Integer.class), eq(cuerpo.nit()))).thenReturn(1);

        // When-Then
        assertThatThrownBy(() -> plataformaService.provisionarEmpresa(cuerpo))
                .isInstanceOf(EmpresaYaExisteException.class);
    }

    @Test
    void provisionarEmpresa_schemaDuplicado_lanzaEmpresaYaExisteException() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("empresa_test", "PasswordAdmin123");
        when(jdbc.queryForObject(eq(SQL_NIT), eq(Integer.class), eq(cuerpo.nit()))).thenReturn(0);
        when(jdbc.queryForObject(eq(SQL_SCHEMA), eq(Integer.class), eq(cuerpo.schemaNombre()))).thenReturn(1);

        // When-Then
        assertThatThrownBy(() -> plataformaService.provisionarEmpresa(cuerpo))
                .isInstanceOf(EmpresaYaExisteException.class);
    }

    @Test
    void provisionarEmpresa_fallaMigracionEsquema_lanzaRuntimeException() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("empresa_test", "PasswordAdmin123");
        when(jdbc.queryForObject(eq(SQL_NIT), eq(Integer.class), eq(cuerpo.nit()))).thenReturn(0);
        when(jdbc.queryForObject(eq(SQL_SCHEMA), eq(Integer.class), eq(cuerpo.schemaNombre()))).thenReturn(0);
        doThrow(new RuntimeException("fallo de flyway"))
                .when(migradorEsquemas).migrarEsquemaEmpresa(cuerpo.schemaNombre());

        // When-Then
        assertThatThrownBy(() -> plataformaService.provisionarEmpresa(cuerpo))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al crear el esquema de la empresa");
        verify(supabaseAdmin, never()).crearUsuario(any(), any(), any(), any(), any(), any());
    }

    @Test
    void provisionarEmpresa_conPasswordPropia_retornaSinPasswordTemporal() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("empresa_test", "PasswordAdmin123");
        var supabaseId = UUID.randomUUID();
        prepararVerificacionesFelices(cuerpo);
        when(supabaseAdmin.crearUsuario(eq(cuerpo.adminEmail()), eq("PasswordAdmin123"),
                eq(cuerpo.adminNombreCompleto()), eq(cuerpo.adminUsername()), eq("ADMIN"),
                eq(cuerpo.schemaNombre())))
                .thenReturn(supabaseId);

        // When
        RespuestaEmpresaCreada actualResultado = plataformaService.provisionarEmpresa(cuerpo);

        // Then
        assertThat(actualResultado.passwordTemporal()).isNull();
        assertThat(actualResultado.adminSupabaseId()).isEqualTo(supabaseId);
        assertThat(actualResultado.schemaNombre()).isEqualTo("empresa_test");
    }

    @Test
    void provisionarEmpresa_sinPasswordPropia_generaPasswordTemporalDeDoceCaracteres() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("empresa_test", null);
        var supabaseId = UUID.randomUUID();
        prepararVerificacionesFelices(cuerpo);
        when(supabaseAdmin.crearUsuario(eq(cuerpo.adminEmail()), anyString(),
                eq(cuerpo.adminNombreCompleto()), eq(cuerpo.adminUsername()), eq("ADMIN"),
                eq(cuerpo.schemaNombre())))
                .thenReturn(supabaseId);

        // When
        RespuestaEmpresaCreada actualResultado = plataformaService.provisionarEmpresa(cuerpo);

        // Then
        assertThat(actualResultado.passwordTemporal()).hasSize(12);
    }

    @Test
    void provisionarEmpresa_fallaCreacionUsuarioEnSupabase_noIntentaRevertirUsuarioInexistente() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("empresa_test", "PasswordAdmin123");
        prepararVerificacionesFelices(cuerpo);
        when(supabaseAdmin.crearUsuario(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("fallo al crear credencial"));

        // When-Then
        assertThatThrownBy(() -> plataformaService.provisionarEmpresa(cuerpo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("fallo al crear credencial");
        verify(supabaseAdmin, never()).eliminarUsuario(any());
    }

    @Test
    void provisionarEmpresa_fallaEscrituraLocal_revierteUsuarioEnSupabaseYRelanzaExcepcion() {
        // Given
        var cuerpo = cuerpoNuevaEmpresa("empresa_test", "PasswordAdmin123");
        var supabaseId = UUID.randomUUID();
        prepararVerificacionesFelices(cuerpo);
        when(supabaseAdmin.crearUsuario(any(), any(), any(), any(), any(), any())).thenReturn(supabaseId);
        when(jdbc.update(anyString(), any(), any(), any(), any())).thenThrow(new RuntimeException("fallo de bd"));

        // When-Then
        assertThatThrownBy(() -> plataformaService.provisionarEmpresa(cuerpo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("fallo de bd");
        verify(supabaseAdmin).eliminarUsuario(supabaseId);
    }

    private void prepararVerificacionesFelices(CuerpoNuevaEmpresa cuerpo) {
        when(jdbc.queryForObject(eq(SQL_NIT), eq(Integer.class), eq(cuerpo.nit()))).thenReturn(0);
        when(jdbc.queryForObject(eq(SQL_SCHEMA), eq(Integer.class), eq(cuerpo.schemaNombre()))).thenReturn(0);
    }

    private CuerpoNuevaEmpresa cuerpoNuevaEmpresa(String schemaNombre, String adminPassword) {
        return new CuerpoNuevaEmpresa(
                "Empresa de Prueba",
                "900123456",
                schemaNombre,
                "admin@empresatest.com",
                "Admin Principal",
                "admin.principal",
                adminPassword);
    }
}
