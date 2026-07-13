package com.recyops.api.inventario.service;

import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.UsuarioAutenticado;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.inventario.dtos.CuerpoAjuste;
import com.recyops.api.inventario.dtos.CuerpoCrearLinea;
import com.recyops.api.inventario.dtos.CuerpoMerma;
import com.recyops.api.inventario.dtos.CuerpoTopes;
import com.recyops.api.inventario.dtos.RespuestaLineaInventario;
import com.recyops.api.inventario.dtos.RespuestaMovimiento;
import com.recyops.api.inventario.entity.LineaInventario;
import com.recyops.api.inventario.entity.MovimientoInventario;
import com.recyops.api.inventario.enums.TipoOperacion;
import com.recyops.api.inventario.excepciones.LineaDuplicadaException;
import com.recyops.api.inventario.excepciones.LineaInventarioNoEncontradaException;
import com.recyops.api.inventario.excepciones.StockInvalidoException;
import com.recyops.api.inventario.interfaces.InventarioService;
import com.recyops.api.inventario.repository.LineaInventarioRepository;
import com.recyops.api.inventario.repository.MovimientoInventarioRepository;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.repository.MaterialRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventarioServiceImpl implements InventarioService {

    private final LineaInventarioRepository lineaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final BodegaRepository bodegaRepository;
    private final MaterialRepository materialRepository;

    public InventarioServiceImpl(LineaInventarioRepository lineaRepository,
            MovimientoInventarioRepository movimientoRepository,
            BodegaRepository bodegaRepository, MaterialRepository materialRepository) {
        this.lineaRepository = lineaRepository;
        this.movimientoRepository = movimientoRepository;
        this.bodegaRepository = bodegaRepository;
        this.materialRepository = materialRepository;
    }

    @Override
    public void registrarEntrada(UUID bodegaId, UUID tipoMaterialId, BigDecimal cantidad, String referencia) {
        LineaInventario linea = lineaRepository
                .findByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)
                .orElseGet(() -> crearLineaAutomatica(bodegaId, tipoMaterialId));
        BigDecimal anterior = linea.getStockActual();
        BigDecimal nueva = anterior.add(cantidad);
        registrarMovimiento(linea, TipoOperacion.ENTRADA, cantidad, anterior, nueva, referencia);
        linea.setStockActual(nueva);
    }

    @Override
    public void registrarSalida(UUID bodegaId, UUID tipoMaterialId, BigDecimal cantidad, String referencia) {
        LineaInventario linea = lineaRepository
                .findByBodegaIdAndTipoMaterialId(bodegaId, tipoMaterialId)
                .orElseThrow(() -> new StockInvalidoException(
                        "No hay linea de inventario para descontar la operacion " + referencia));
        BigDecimal anterior = linea.getStockActual();
        if (cantidad.compareTo(anterior) > 0) {
            throw new StockInvalidoException("La salida (" + cantidad + " kg) de la operacion " + referencia
                    + " supera el stock actual (" + anterior + " kg)");
        }
        BigDecimal nueva = anterior.subtract(cantidad);
        registrarMovimiento(linea, TipoOperacion.SALIDA, cantidad, anterior, nueva, referencia);
        linea.setStockActual(nueva);
    }

    /** Linea creada por la primera entrega de un material: topes en 0 hasta que el admin los defina. */
    private LineaInventario crearLineaAutomatica(UUID bodegaId, UUID tipoMaterialId) {
        var bodega = bodegaRepository.findById(bodegaId)
                .orElseThrow(() -> new BodegaNoEncontradaException(bodegaId));
        var material = materialRepository.findById(tipoMaterialId)
                .orElseThrow(() -> new MaterialNoEncontradoException(tipoMaterialId));
        return lineaRepository.save(LineaInventario.builder()
                .bodega(bodega)
                .tipoMaterial(material)
                .stockMinimo(BigDecimal.ZERO)
                .stockMaximo(BigDecimal.ZERO)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaLineaInventario> listar(UUID bodegaId, UUID tipoMaterialId,
            Boolean bajoMinimo, int page, int size) {
        var pagina = lineaRepository.buscar(bodegaId, tipoMaterialId, bajoMinimo, PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaLineaInventario::desde);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaLineaInventario obtener(UUID id) {
        return RespuestaLineaInventario.desde(buscarLinea(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaMovimiento> movimientos(UUID lineaId, int page, int size) {
        buscarLinea(lineaId);
        var pagina = movimientoRepository.findByLineaIdOrderByFechaRegistroDesc(lineaId,
                PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaMovimiento::desde);
    }

    @Override
    public RespuestaLineaInventario crearLinea(CuerpoCrearLinea cuerpo) {
        if (lineaRepository.existsByBodegaIdAndTipoMaterialId(cuerpo.bodegaId(), cuerpo.tipoMaterialId())) {
            throw new LineaDuplicadaException();
        }
        validarTopes(cuerpo.stockMinimo(), cuerpo.stockMaximo());
        var bodega = bodegaRepository.findById(cuerpo.bodegaId())
                .orElseThrow(() -> new BodegaNoEncontradaException(cuerpo.bodegaId()));
        var material = materialRepository.findById(cuerpo.tipoMaterialId())
                .orElseThrow(() -> new MaterialNoEncontradoException(cuerpo.tipoMaterialId()));

        LineaInventario linea = LineaInventario.builder()
                .bodega(bodega)
                .tipoMaterial(material)
                .stockMinimo(cuerpo.stockMinimo())
                .stockMaximo(cuerpo.stockMaximo())
                .build();
        return RespuestaLineaInventario.desde(lineaRepository.save(linea));
    }

    @Override
    public RespuestaLineaInventario actualizarTopes(UUID id, CuerpoTopes cuerpo) {
        validarTopes(cuerpo.stockMinimo(), cuerpo.stockMaximo());
        LineaInventario linea = buscarLinea(id);
        linea.setStockMinimo(cuerpo.stockMinimo());
        linea.setStockMaximo(cuerpo.stockMaximo());
        return RespuestaLineaInventario.desde(linea);
    }

    @Override
    public RespuestaLineaInventario registrarAjuste(UUID id, CuerpoAjuste cuerpo) {
        LineaInventario linea = buscarLinea(id);
        BigDecimal anterior = linea.getStockActual();
        BigDecimal nueva = cuerpo.cantidadNueva();
        registrarMovimiento(linea, TipoOperacion.AJUSTE, nueva.subtract(anterior).abs(), anterior, nueva,
                cuerpo.motivo());
        linea.setStockActual(nueva);
        return RespuestaLineaInventario.desde(linea);
    }

    @Override
    public RespuestaLineaInventario registrarMerma(UUID id, CuerpoMerma cuerpo) {
        LineaInventario linea = buscarLinea(id);
        BigDecimal anterior = linea.getStockActual();
        if (cuerpo.cantidad().compareTo(anterior) > 0) {
            throw new StockInvalidoException(
                    "La merma (" + cuerpo.cantidad() + ") supera el stock actual (" + anterior + ")");
        }
        BigDecimal nueva = anterior.subtract(cuerpo.cantidad());
        registrarMovimiento(linea, TipoOperacion.MERMA, cuerpo.cantidad(), anterior, nueva, cuerpo.motivo());
        linea.setStockActual(nueva);
        return RespuestaLineaInventario.desde(linea);
    }

    private void registrarMovimiento(LineaInventario linea, TipoOperacion tipo, BigDecimal cantidad,
            BigDecimal anterior, BigDecimal nueva, String referencia) {
        movimientoRepository.save(MovimientoInventario.builder()
                .linea(linea)
                .tipoOperacion(tipo)
                .cantidad(cantidad)
                .cantidadAnterior(anterior)
                .cantidadNueva(nueva)
                .referencia(referencia)
                .usuarioNombre(UsuarioAutenticado.nombreCompleto())
                .build());
    }

    private void validarTopes(BigDecimal minimo, BigDecimal maximo) {
        if (minimo.compareTo(maximo) > 0) {
            throw new StockInvalidoException("El stock minimo no puede ser mayor que el stock maximo");
        }
    }

    private LineaInventario buscarLinea(UUID id) {
        return lineaRepository.findById(id).orElseThrow(() -> new LineaInventarioNoEncontradaException(id));
    }
}
