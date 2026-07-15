package com.recyops.api.comun.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un metodo de servicio para que el {@link AspectoLogTransaccional}
 * registre su ejecucion en logs/transacciones.log.
 * Solo se loguean metadatos (operacion, usuario, empresa, duracion y resultado);
 * nunca argumentos ni valores de retorno.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogTransaccional {
    String operacion();
}
