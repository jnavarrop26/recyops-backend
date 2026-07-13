package com.recyops.api.inventario.interfaces;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.inventario.dtos.CuerpoAjuste;
import com.recyops.api.inventario.dtos.CuerpoCrearLinea;
import com.recyops.api.inventario.dtos.CuerpoMerma;
import com.recyops.api.inventario.dtos.CuerpoTopes;
import com.recyops.api.inventario.dtos.RespuestaLineaInventario;
import com.recyops.api.inventario.dtos.RespuestaMovimiento;
import java.math.BigDecimal;
import java.util.UUID;

public interface InventarioService {

    /**
     * Suma stock por un documento (ej. recepcion de una entrega). Si el material
     * no tiene linea en esa bodega, la crea automaticamente con topes en cero.
     * El movimiento ENTRADA queda referenciado al documento origen.
     */
    void registrarEntrada(UUID bodegaId, UUID tipoMaterialId, BigDecimal cantidad, String referencia);

    /**
     * Descuenta stock por un documento (ej. despacho o reversion de una entrega).
     * Falla si el stock disponible no alcanza.
     */
    void registrarSalida(UUID bodegaId, UUID tipoMaterialId, BigDecimal cantidad, String referencia);

    RespuestaPagina<RespuestaLineaInventario> listar(UUID bodegaId, UUID tipoMaterialId,
            Boolean bajoMinimo, int page, int size);

    RespuestaLineaInventario obtener(UUID id);

    RespuestaPagina<RespuestaMovimiento> movimientos(UUID lineaId, int page, int size);

    RespuestaLineaInventario crearLinea(CuerpoCrearLinea cuerpo);

    RespuestaLineaInventario actualizarTopes(UUID id, CuerpoTopes cuerpo);

    RespuestaLineaInventario registrarAjuste(UUID id, CuerpoAjuste cuerpo);

    RespuestaLineaInventario registrarMerma(UUID id, CuerpoMerma cuerpo);
}
