package com.recyops.api.auth.interfaces;

import com.recyops.api.auth.dtos.CuerpoLogin;
import com.recyops.api.auth.dtos.CuerpoRecuperar;
import com.recyops.api.auth.dtos.CuerpoRefresh;
import com.recyops.api.auth.dtos.CuerpoRestablecer;
import com.recyops.api.auth.dtos.RespuestaLogin;

public interface AuthService {

    RespuestaLogin login(CuerpoLogin cuerpo);

    /** Renueva la sesion antes de que venza el access token (Supabase rota el refresh token). */
    RespuestaLogin refrescar(CuerpoRefresh cuerpo);

    /** Pide a Supabase el correo de recuperacion; responde igual exista o no el correo. */
    void recuperarPassword(CuerpoRecuperar cuerpo);

    /** Fija la nueva contrasena con el token de recuperacion del correo. */
    void restablecerPassword(CuerpoRestablecer cuerpo);
}
