package com.recyops.api.comun.excepciones;

import com.recyops.api.comun.dtos.ErrorApi;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de todos los modulos al cuerpo {@link ErrorApi}.
 * El cliente recibe un error limpio y controlado; el detalle completo
 * (stack trace, ruta, metodo) queda siempre en los logs (errores.log).
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @ExceptionHandler(ExcepcionRecyOps.class)
    public ResponseEntity<ErrorApi> manejarNegocio(ExcepcionRecyOps ex, HttpServletRequest peticion) {
        HttpStatus estado = ex.getEstado();
        // WARN con stack trace: el detalle completo del error queda en los logs
        log.warn("Error de negocio {} {} en {} {} -> {}", estado.value(), ex.getClass().getSimpleName(),
                peticion.getMethod(), peticion.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(estado)
                .body(ErrorApi.de(estado.value(), estado.getReasonPhrase(), ex.getMessage(), peticion.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException ex,
            HttpServletRequest peticion) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validacion fallida en {} {} -> {}", peticion.getMethod(), peticion.getRequestURI(), detalle);
        return ResponseEntity.badRequest()
                .body(ErrorApi.de(400, "Validacion fallida", detalle, peticion.getRequestURI()));
    }

    /** Rechazos de @PreAuthorize (p. ej. un operario pidiendo recursos de admin). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> manejarAccesoDenegado(AccessDeniedException ex,
            HttpServletRequest peticion) {
        log.warn("Acceso denegado en {} {}", peticion.getMethod(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApi.de(403, "Forbidden", "No tienes permisos para esta accion",
                        peticion.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarInesperado(Exception ex, HttpServletRequest peticion) {
        log.error("Error no controlado en {} {}", peticion.getMethod(), peticion.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(ErrorApi.de(500, "Error interno", "Ocurrio un error inesperado", peticion.getRequestURI()));
    }
}
