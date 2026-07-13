package com.recyops.api.tenant;

import java.util.regex.Pattern;

/**
 * Guarda, por hilo de peticion, el esquema de Postgres de la empresa autenticada.
 * El nombre se valida contra un patron estricto porque se usa en SET search_path.
 */
public final class ContextoEmpresa {

    private static final ThreadLocal<String> ESQUEMA_ACTUAL = new ThreadLocal<>();
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    private ContextoEmpresa() {
    }

    public static void establecer(String esquema) {
        if (esquema == null || !NOMBRE_VALIDO.matcher(esquema).matches()) {
            throw new IllegalArgumentException("Nombre de esquema de empresa invalido: " + esquema);
        }
        ESQUEMA_ACTUAL.set(esquema);
    }

    public static String obtener() {
        return ESQUEMA_ACTUAL.get();
    }

    public static void limpiar() {
        ESQUEMA_ACTUAL.remove();
    }

    public static boolean esNombreValido(String esquema) {
        return esquema != null && NOMBRE_VALIDO.matcher(esquema).matches();
    }
}
