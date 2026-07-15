package com.recyops.api.comun.log;

import com.recyops.api.comun.UsuarioAutenticado;
import com.recyops.api.tenant.ContextoEmpresa;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra operaciones de negocio en logs/transacciones.log (logger TRANSACCIONES).
 * Solo captura metadatos: nombre de operacion, usuario autenticado, esquema de
 * empresa, duracion y si tuvo exito. Nunca loguea argumentos ni valores de retorno
 * para evitar filtrar datos sensibles (contrasenas, cedulas, tokens).
 */
@Aspect
@Component
public class AspectoLogTransaccional {

    private static final Logger log = LoggerFactory.getLogger("TRANSACCIONES");

    @Around("@annotation(logTransaccional)")
    public Object registrar(ProceedingJoinPoint pjp, LogTransaccional logTransaccional)
            throws Throwable {
        String usuario = UsuarioAutenticado.nombreCompleto();
        String empresa = ContextoEmpresa.obtener();
        long inicio = System.currentTimeMillis();
        try {
            Object resultado = pjp.proceed();
            log.info("op={} usuario={} empresa={} ms={} ok=true",
                    logTransaccional.operacion(), usuario, empresa,
                    System.currentTimeMillis() - inicio);
            return resultado;
        } catch (Exception ex) {
            log.warn("op={} usuario={} empresa={} ms={} ok=false error={}",
                    logTransaccional.operacion(), usuario, empresa,
                    System.currentTimeMillis() - inicio,
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
