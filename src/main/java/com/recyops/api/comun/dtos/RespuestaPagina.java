package com.recyops.api.comun.dtos;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Pagina con la forma exacta que espera el cliente RecyOps:
 * { content, totalElements, totalPages, number, size }.
 */
public record RespuestaPagina<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size) {

    public static <E, T> RespuestaPagina<T> desde(Page<E> pagina, Function<E, T> mapeo) {
        return new RespuestaPagina<>(
                pagina.getContent().stream().map(mapeo).toList(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.getNumber(),
                pagina.getSize());
    }
}
