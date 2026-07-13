package com.recyops.api.usuario.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

/** Fallo al hablar con la Admin API de Supabase Auth (el detalle queda en los logs). */
public class ErrorSupabaseAuthException extends ExcepcionRecyOps {

    public ErrorSupabaseAuthException() {
        super(HttpStatus.BAD_GATEWAY, "No se pudo crear la credencial del usuario en Supabase; intenta de nuevo");
    }
}
