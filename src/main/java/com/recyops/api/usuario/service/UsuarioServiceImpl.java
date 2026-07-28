package com.recyops.api.usuario.service;

import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.log.LogTransaccional;
import com.recyops.api.config.PropiedadesSupabase;
import com.recyops.api.tenant.ContextoEmpresa;
import com.recyops.api.usuario.dtos.CuerpoEditarTrabajador;
import com.recyops.api.usuario.dtos.CuerpoTrabajador;
import com.recyops.api.usuario.dtos.RespuestaRol;
import com.recyops.api.usuario.dtos.RespuestaTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajadorCreado;
import com.recyops.api.usuario.dtos.RespuestaUsuarioBodega;
import com.recyops.api.usuario.entity.Usuario;
import com.recyops.api.usuario.enums.EstadoUsuario;
import com.recyops.api.usuario.excepciones.RolNoEncontradoException;
import com.recyops.api.usuario.excepciones.UsuarioDuplicadoException;
import com.recyops.api.usuario.excepciones.UsuarioNoEncontradoException;
import com.recyops.api.usuario.interfaces.UsuarioService;
import com.recyops.api.usuario.repository.RolRepository;
import com.recyops.api.usuario.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private static final String CHARS_PASSWORD =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final BodegaRepository bodegaRepository;
    private final ClienteSupabaseAdmin supabaseAdmin;
    private final PropiedadesSupabase propiedadesSupabase;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            BodegaRepository bodegaRepository, ClienteSupabaseAdmin supabaseAdmin,
            PropiedadesSupabase propiedadesSupabase) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.bodegaRepository = bodegaRepository;
        this.supabaseAdmin = supabaseAdmin;
        this.propiedadesSupabase = propiedadesSupabase;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaTrabajador> listarTrabajadores() {
        return usuarioRepository.listarConRolYBodega().stream().map(RespuestaTrabajador::desde).toList();
    }

    @Override
    @LogTransaccional(operacion = "TRABAJADOR_REGISTRADO")
    public RespuestaTrabajadorCreado registrarTrabajador(CuerpoTrabajador cuerpo) {
        if (usuarioRepository.existsByUsername(cuerpo.username())) {
            throw new UsuarioDuplicadoException("username", cuerpo.username());
        }
        if (usuarioRepository.existsByEmail(cuerpo.email())) {
            throw new UsuarioDuplicadoException("email", cuerpo.email());
        }
        var rol = rolRepository.findById(cuerpo.rolId())
                .orElseThrow(() -> new RolNoEncontradoException(cuerpo.rolId()));
        var bodega = bodegaRepository.findById(cuerpo.bodegaId())
                .orElseThrow(() -> new BodegaNoEncontradaException(cuerpo.bodegaId()));

        boolean passwordPropia = cuerpo.password() != null && !cuerpo.password().isBlank();
        String passwordTemporal = passwordPropia ? null : generarPasswordTemporal();
        String passwordDefinitiva = passwordPropia ? cuerpo.password() : passwordTemporal;

        // El nuevo trabajador queda en el mismo esquema de la empresa del admin
        // que lo registra; el claim viaja en cada JWT que Supabase le emita.
        String esquemaEmpresa = ContextoEmpresa.obtener() != null
                ? ContextoEmpresa.obtener()
                : propiedadesSupabase.esquemaPorDefecto();

        UUID supabaseId = supabaseAdmin.crearUsuario(
                cuerpo.email(),
                passwordDefinitiva,
                cuerpo.nombreCompleto(),
                cuerpo.username(),
                rol.getNombre(),
                esquemaEmpresa);

        Usuario usuario = Usuario.builder()
                .supabaseId(supabaseId)
                .nombreCompleto(cuerpo.nombreCompleto())
                .username(cuerpo.username())
                .email(cuerpo.email())
                .telefono(cuerpo.telefono())
                .rol(rol)
                .bodega(bodega)
                .build();
        try {
            usuario = usuarioRepository.save(usuario);
            usuarioRepository.flush();
        } catch (RuntimeException ex) {
            // La credencial ya existe en Supabase pero el perfil local fallo:
            // se revierte alla para no dejar usuarios huerfanos.
            supabaseAdmin.eliminarUsuario(supabaseId);
            throw ex;
        }
        return RespuestaTrabajadorCreado.desde(usuario, passwordTemporal);
    }

    @Override
    public RespuestaTrabajador editarTrabajador(UUID id, CuerpoEditarTrabajador cuerpo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
        var rol = rolRepository.findById(cuerpo.rolId())
                .orElseThrow(() -> new RolNoEncontradoException(cuerpo.rolId()));
        var bodega = bodegaRepository.findById(cuerpo.bodegaId())
                .orElseThrow(() -> new BodegaNoEncontradaException(cuerpo.bodegaId()));

        // Primero Supabase (los proximos JWT deben traer el rol nuevo);
        // si falla, no se toca el perfil local.
        if (usuario.getSupabaseId() != null) {
            supabaseAdmin.actualizarUsuario(usuario.getSupabaseId(),
                    cuerpo.nombreCompleto(), rol.getNombre());
        }

        usuario.setNombreCompleto(cuerpo.nombreCompleto());
        usuario.setTelefono(cuerpo.telefono());
        usuario.setRol(rol);
        usuario.setBodega(bodega);
        return RespuestaTrabajador.desde(usuario);
    }

    @Override
    @LogTransaccional(operacion = "USUARIO_ESTADO_CAMBIADO")
    public RespuestaTrabajador cambiarEstado(UUID id, EstadoUsuario valor) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));

        // Desactivar tambien bloquea el login en Supabase; reactivar lo restaura.
        if (usuario.getSupabaseId() != null) {
            supabaseAdmin.cambiarBloqueo(usuario.getSupabaseId(), valor == EstadoUsuario.INACTIVO);
        }

        usuario.setEstado(valor);
        return RespuestaTrabajador.desde(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaRol> listarRoles() {
        return rolRepository.findAll().stream().map(RespuestaRol::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaUsuarioBodega> listarPorBodega(UUID bodegaId) {
        return usuarioRepository.findByBodegaId(bodegaId).stream()
                .map(RespuestaUsuarioBodega::desde)
                .toList();
    }

    private String generarPasswordTemporal() {
        return SECURE_RANDOM.ints(12, 0, CHARS_PASSWORD.length())
                .mapToObj(i -> String.valueOf(CHARS_PASSWORD.charAt(i)))
                .collect(Collectors.joining());
    }
}
