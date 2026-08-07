package com.recyops.api.unit.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.config.PropiedadesSupabase;
import com.recyops.api.tenant.ContextoEmpresa;
import com.recyops.api.usuario.dtos.CuerpoEditarTrabajador;
import com.recyops.api.usuario.dtos.CuerpoTrabajador;
import com.recyops.api.usuario.dtos.RespuestaRol;
import com.recyops.api.usuario.dtos.RespuestaTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajadorCreado;
import com.recyops.api.usuario.dtos.RespuestaUsuarioBodega;
import com.recyops.api.usuario.entity.Rol;
import com.recyops.api.usuario.entity.Usuario;
import com.recyops.api.usuario.enums.EstadoUsuario;
import com.recyops.api.usuario.excepciones.RolNoEncontradoException;
import com.recyops.api.usuario.excepciones.UsuarioDuplicadoException;
import com.recyops.api.usuario.excepciones.UsuarioNoEncontradoException;
import com.recyops.api.usuario.repository.RolRepository;
import com.recyops.api.usuario.repository.UsuarioRepository;
import com.recyops.api.usuario.service.ClienteSupabaseAdmin;
import com.recyops.api.usuario.service.UsuarioServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @Mock
    private ClienteSupabaseAdmin supabaseAdmin;

    @Mock
    private PropiedadesSupabase propiedadesSupabase;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @AfterEach
    void limpiarContextoEmpresa() {
        // El ThreadLocal de ContextoEmpresa sobrevive entre tests del mismo hilo
        // si no se limpia explicitamente.
        ContextoEmpresa.limpiar();
    }

    @Test
    void listarTrabajadores_paginaValida_retornaPaginaMapeada() {
        // Given
        var usuario = usuarioBase(UUID.randomUUID(), UUID.randomUUID());
        Page<Usuario> pagina = new PageImpl<>(List.of(usuario), PageRequest.of(0, 20), 1);
        when(usuarioRepository.listarConRolYBodega(any(PageRequest.class))).thenReturn(pagina);

        // When
        RespuestaPagina<RespuestaTrabajador> actualResultado = usuarioService.listarTrabajadores(0, 20);

        // Then
        assertThat(actualResultado.content()).hasSize(1);
        assertThat(actualResultado.content().get(0).username()).isEqualTo("jperez");
    }

    @Test
    void registrarTrabajador_usernameDuplicado_lanzaUsuarioDuplicadoException() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        when(usuarioRepository.existsByUsername(cuerpo.username())).thenReturn(true);

        // When-Then
        assertThatThrownBy(() -> usuarioService.registrarTrabajador(cuerpo))
                .isInstanceOf(UsuarioDuplicadoException.class);
    }

    @Test
    void registrarTrabajador_emailDuplicado_lanzaUsuarioDuplicadoException() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        when(usuarioRepository.existsByUsername(cuerpo.username())).thenReturn(false);
        when(usuarioRepository.existsByEmail(cuerpo.email())).thenReturn(true);

        // When-Then
        assertThatThrownBy(() -> usuarioService.registrarTrabajador(cuerpo))
                .isInstanceOf(UsuarioDuplicadoException.class);
    }

    @Test
    void registrarTrabajador_rolInexistente_lanzaRolNoEncontradoException() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        when(usuarioRepository.existsByUsername(cuerpo.username())).thenReturn(false);
        when(usuarioRepository.existsByEmail(cuerpo.email())).thenReturn(false);
        when(rolRepository.findById(cuerpo.rolId())).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> usuarioService.registrarTrabajador(cuerpo))
                .isInstanceOf(RolNoEncontradoException.class);
    }

    @Test
    void registrarTrabajador_bodegaInexistente_lanzaBodegaNoEncontradaException() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        var rol = Rol.builder().id(cuerpo.rolId()).nombre("OPERARIO").build();
        when(usuarioRepository.existsByUsername(cuerpo.username())).thenReturn(false);
        when(usuarioRepository.existsByEmail(cuerpo.email())).thenReturn(false);
        when(rolRepository.findById(cuerpo.rolId())).thenReturn(Optional.of(rol));
        when(bodegaRepository.findById(cuerpo.bodegaId())).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> usuarioService.registrarTrabajador(cuerpo))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    @Test
    void registrarTrabajador_conPasswordPropia_noGeneraPasswordTemporal() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        prepararDependenciasFelices(cuerpo);
        when(supabaseAdmin.crearUsuario(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RespuestaTrabajadorCreado actualResultado = usuarioService.registrarTrabajador(cuerpo);

        // Then
        assertThat(actualResultado.passwordTemporal()).isNull();
        verify(supabaseAdmin).crearUsuario(eq(cuerpo.email()), eq("MiPassword123"),
                eq(cuerpo.nombreCompleto()), eq(cuerpo.username()), eq("OPERARIO"), any());
    }

    @Test
    void registrarTrabajador_sinPassword_generaPasswordTemporal() {
        // Given
        var cuerpo = cuerpoTrabajador(null);
        prepararDependenciasFelices(cuerpo);
        var passwordCaptor = ArgumentCaptor.forClass(String.class);
        when(supabaseAdmin.crearUsuario(any(), passwordCaptor.capture(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RespuestaTrabajadorCreado actualResultado = usuarioService.registrarTrabajador(cuerpo);

        // Then
        assertThat(actualResultado.passwordTemporal()).hasSize(12);
        assertThat(actualResultado.passwordTemporal()).isEqualTo(passwordCaptor.getValue());
    }

    @Test
    void registrarTrabajador_conEsquemaEnContexto_usaEsquemaDelContexto() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        prepararDependenciasFelices(cuerpo);
        ContextoEmpresa.establecer("empresa_test");
        var esquemaCaptor = ArgumentCaptor.forClass(String.class);
        when(supabaseAdmin.crearUsuario(any(), any(), any(), any(), any(), esquemaCaptor.capture()))
                .thenReturn(UUID.randomUUID());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        usuarioService.registrarTrabajador(cuerpo);

        // Then
        assertThat(esquemaCaptor.getValue()).isEqualTo("empresa_test");
    }

    @Test
    void registrarTrabajador_sinEsquemaEnContexto_usaEsquemaPorDefecto() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        prepararDependenciasFelices(cuerpo);
        when(propiedadesSupabase.esquemaPorDefecto()).thenReturn("empresa_demo");
        var esquemaCaptor = ArgumentCaptor.forClass(String.class);
        when(supabaseAdmin.crearUsuario(any(), any(), any(), any(), any(), esquemaCaptor.capture()))
                .thenReturn(UUID.randomUUID());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        usuarioService.registrarTrabajador(cuerpo);

        // Then
        assertThat(esquemaCaptor.getValue()).isEqualTo("empresa_demo");
    }

    @Test
    void registrarTrabajador_fallaGuardadoLocal_revierteUsuarioEnSupabase() {
        // Given
        var cuerpo = cuerpoTrabajador("MiPassword123");
        prepararDependenciasFelices(cuerpo);
        var supabaseId = UUID.randomUUID();
        when(supabaseAdmin.crearUsuario(any(), any(), any(), any(), any(), any())).thenReturn(supabaseId);
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("fallo de base de datos"));

        // When-Then
        assertThatThrownBy(() -> usuarioService.registrarTrabajador(cuerpo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("fallo de base de datos");
        verify(supabaseAdmin).eliminarUsuario(supabaseId);
    }

    @Test
    void editarTrabajador_idInexistente_lanzaUsuarioNoEncontradoException() {
        // Given
        var id = UUID.randomUUID();
        var cuerpo = cuerpoEditarTrabajador();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> usuarioService.editarTrabajador(id, cuerpo))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void editarTrabajador_rolInexistente_lanzaRolNoEncontradoException() {
        // Given
        var id = UUID.randomUUID();
        var cuerpo = cuerpoEditarTrabajador();
        var usuario = usuarioBase(UUID.randomUUID(), UUID.randomUUID());
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(cuerpo.rolId())).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> usuarioService.editarTrabajador(id, cuerpo))
                .isInstanceOf(RolNoEncontradoException.class);
    }

    @Test
    void editarTrabajador_bodegaInexistente_lanzaBodegaNoEncontradaException() {
        // Given
        var id = UUID.randomUUID();
        var cuerpo = cuerpoEditarTrabajador();
        var usuario = usuarioBase(UUID.randomUUID(), UUID.randomUUID());
        var nuevoRol = Rol.builder().id(cuerpo.rolId()).nombre("ADMIN").build();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(cuerpo.rolId())).thenReturn(Optional.of(nuevoRol));
        when(bodegaRepository.findById(cuerpo.bodegaId())).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> usuarioService.editarTrabajador(id, cuerpo))
                .isInstanceOf(BodegaNoEncontradaException.class);
    }

    @Test
    void editarTrabajador_conSupabaseIdAsociado_sincronizaEnSupabase() {
        // Given
        var id = UUID.randomUUID();
        var supabaseId = UUID.randomUUID();
        var cuerpo = cuerpoEditarTrabajador();
        var usuario = usuarioBase(id, UUID.randomUUID());
        usuario.setSupabaseId(supabaseId);
        var nuevoRol = Rol.builder().id(cuerpo.rolId()).nombre("ADMIN").build();
        var nuevaBodega = Bodega.builder().id(cuerpo.bodegaId()).nombre("Bodega Norte").build();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(cuerpo.rolId())).thenReturn(Optional.of(nuevoRol));
        when(bodegaRepository.findById(cuerpo.bodegaId())).thenReturn(Optional.of(nuevaBodega));

        // When
        RespuestaTrabajador actualResultado = usuarioService.editarTrabajador(id, cuerpo);

        // Then
        verify(supabaseAdmin).actualizarUsuario(supabaseId, cuerpo.nombreCompleto(), "ADMIN");
        assertThat(actualResultado.rolNombre()).isEqualTo("ADMIN");
        assertThat(actualResultado.bodegaNombre()).isEqualTo("Bodega Norte");
    }

    @Test
    void editarTrabajador_sinSupabaseIdAsociado_noSincronizaEnSupabase() {
        // Given
        var id = UUID.randomUUID();
        var cuerpo = cuerpoEditarTrabajador();
        var usuario = usuarioBase(id, UUID.randomUUID());
        usuario.setSupabaseId(null);
        var nuevoRol = Rol.builder().id(cuerpo.rolId()).nombre("ADMIN").build();
        var nuevaBodega = Bodega.builder().id(cuerpo.bodegaId()).nombre("Bodega Norte").build();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(cuerpo.rolId())).thenReturn(Optional.of(nuevoRol));
        when(bodegaRepository.findById(cuerpo.bodegaId())).thenReturn(Optional.of(nuevaBodega));

        // When
        usuarioService.editarTrabajador(id, cuerpo);

        // Then
        verify(supabaseAdmin, never()).actualizarUsuario(any(), any(), any());
    }

    @Test
    void cambiarEstado_idInexistente_lanzaUsuarioNoEncontradoException() {
        // Given
        var id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> usuarioService.cambiarEstado(id, EstadoUsuario.INACTIVO))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void cambiarEstado_aInactivoConSupabaseId_bloqueaEnSupabase() {
        // Given
        var id = UUID.randomUUID();
        var supabaseId = UUID.randomUUID();
        var usuario = usuarioBase(id, UUID.randomUUID());
        usuario.setSupabaseId(supabaseId);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        // When
        RespuestaTrabajador actualResultado = usuarioService.cambiarEstado(id, EstadoUsuario.INACTIVO);

        // Then
        verify(supabaseAdmin).cambiarBloqueo(supabaseId, true);
        assertThat(actualResultado.estado()).isEqualTo("INACTIVO");
    }

    @Test
    void cambiarEstado_aActivoConSupabaseId_desbloqueaEnSupabase() {
        // Given
        var id = UUID.randomUUID();
        var supabaseId = UUID.randomUUID();
        var usuario = usuarioBase(id, UUID.randomUUID());
        usuario.setSupabaseId(supabaseId);
        usuario.setEstado(EstadoUsuario.INACTIVO);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        // When
        RespuestaTrabajador actualResultado = usuarioService.cambiarEstado(id, EstadoUsuario.ACTIVO);

        // Then
        verify(supabaseAdmin).cambiarBloqueo(supabaseId, false);
        assertThat(actualResultado.estado()).isEqualTo("ACTIVO");
    }

    @Test
    void cambiarEstado_sinSupabaseId_noLlamaASupabase() {
        // Given
        var id = UUID.randomUUID();
        var usuario = usuarioBase(id, UUID.randomUUID());
        usuario.setSupabaseId(null);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        // When
        usuarioService.cambiarEstado(id, EstadoUsuario.INACTIVO);

        // Then
        verify(supabaseAdmin, never()).cambiarBloqueo(any(), anyBoolean());
    }

    @Test
    void listarRoles_existenRoles_retornaListaMapeada() {
        // Given
        var rol = Rol.builder().id(UUID.randomUUID()).nombre("ADMIN").build();
        when(rolRepository.findAll()).thenReturn(List.of(rol));

        // When
        List<RespuestaRol> actualRoles = usuarioService.listarRoles();

        // Then
        assertThat(actualRoles).extracting(RespuestaRol::nombre).containsExactly("ADMIN");
    }

    @Test
    void listarPorBodega_existenUsuarios_retornaListaMapeada() {
        // Given
        var bodegaId = UUID.randomUUID();
        var usuario = usuarioBase(UUID.randomUUID(), bodegaId);
        when(usuarioRepository.findByBodegaId(bodegaId)).thenReturn(List.of(usuario));

        // When
        List<RespuestaUsuarioBodega> actualUsuarios = usuarioService.listarPorBodega(bodegaId);

        // Then
        assertThat(actualUsuarios).extracting(RespuestaUsuarioBodega::username).containsExactly("jperez");
    }

    private CuerpoTrabajador cuerpoTrabajador(String password) {
        return new CuerpoTrabajador("Juan Perez", "jperez", "jperez@test.com", "3000000000",
                UUID.randomUUID(), UUID.randomUUID(), password);
    }

    private CuerpoEditarTrabajador cuerpoEditarTrabajador() {
        return new CuerpoEditarTrabajador("Juan Perez Editado", "3000000001",
                UUID.randomUUID(), UUID.randomUUID());
    }

    private void prepararDependenciasFelices(CuerpoTrabajador cuerpo) {
        var rol = Rol.builder().id(cuerpo.rolId()).nombre("OPERARIO").build();
        var bodega = Bodega.builder().id(cuerpo.bodegaId()).nombre("Bodega Central").build();
        when(usuarioRepository.existsByUsername(cuerpo.username())).thenReturn(false);
        when(usuarioRepository.existsByEmail(cuerpo.email())).thenReturn(false);
        when(rolRepository.findById(cuerpo.rolId())).thenReturn(Optional.of(rol));
        when(bodegaRepository.findById(cuerpo.bodegaId())).thenReturn(Optional.of(bodega));
    }

    private Usuario usuarioBase(UUID id, UUID bodegaId) {
        var rol = Rol.builder().id(UUID.randomUUID()).nombre("OPERARIO").build();
        var bodega = Bodega.builder().id(bodegaId).nombre("Bodega Central").build();
        return Usuario.builder()
                .id(id)
                .nombreCompleto("Juan Perez")
                .username("jperez")
                .email("jperez@test.com")
                .estado(EstadoUsuario.ACTIVO)
                .rol(rol)
                .bodega(bodega)
                .build();
    }
}
