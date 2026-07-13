package com.recyops.api.usuario.interfaces;

import com.recyops.api.usuario.dtos.CuerpoEditarTrabajador;
import com.recyops.api.usuario.dtos.CuerpoTrabajador;
import com.recyops.api.usuario.dtos.RespuestaRol;
import com.recyops.api.usuario.dtos.RespuestaTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajadorCreado;
import com.recyops.api.usuario.dtos.RespuestaUsuarioBodega;
import com.recyops.api.usuario.enums.EstadoUsuario;
import java.util.List;
import java.util.UUID;

public interface UsuarioService {

    List<RespuestaTrabajador> listarTrabajadores();

    RespuestaTrabajadorCreado registrarTrabajador(CuerpoTrabajador cuerpo);

    /** Edita nombre, telefono, bodega y rol; el cambio de rol se sincroniza en Supabase. */
    RespuestaTrabajador editarTrabajador(UUID id, CuerpoEditarTrabajador cuerpo);

    /** ACTIVO/INACTIVO local + bloqueo/desbloqueo del login en Supabase. */
    RespuestaTrabajador cambiarEstado(UUID id, EstadoUsuario valor);

    List<RespuestaRol> listarRoles();

    List<RespuestaUsuarioBodega> listarPorBodega(UUID bodegaId);
}
