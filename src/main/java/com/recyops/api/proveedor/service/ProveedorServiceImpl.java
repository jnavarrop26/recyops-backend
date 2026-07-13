package com.recyops.api.proveedor.service;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.entrega.repository.EntregaRepository;
import com.recyops.api.proveedor.dtos.CuerpoProveedor;
import com.recyops.api.proveedor.dtos.RespuestaEntregaProveedor;
import com.recyops.api.proveedor.dtos.RespuestaProveedor;
import com.recyops.api.proveedor.entity.Proveedor;
import com.recyops.api.proveedor.enums.EstadoProveedor;
import com.recyops.api.proveedor.excepciones.CalificacionInvalidaException;
import com.recyops.api.proveedor.excepciones.ProveedorNoEncontradoException;
import com.recyops.api.proveedor.interfaces.ProveedorService;
import com.recyops.api.proveedor.repository.ProveedorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final EntregaRepository entregaRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository,
            EntregaRepository entregaRepository) {
        this.proveedorRepository = proveedorRepository;
        this.entregaRepository = entregaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaProveedor> listar(EstadoProveedor estado, String nombre,
            BigDecimal calificacionMin, int page, int size) {
        var pagina = proveedorRepository.buscar(estado, nombre, calificacionMin, PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaProveedor::desde);
    }

    @Override
    public RespuestaProveedor crear(CuerpoProveedor cuerpo) {
        Proveedor proveedor = Proveedor.builder()
                .nombre(cuerpo.nombre())
                .nit(cuerpo.nit())
                .contacto(cuerpo.contacto())
                .telefono(cuerpo.telefono())
                .email(cuerpo.email())
                .direccion(cuerpo.direccion())
                .build();
        return RespuestaProveedor.desde(proveedorRepository.save(proveedor));
    }

    @Override
    public RespuestaProveedor actualizar(UUID id, CuerpoProveedor cuerpo) {
        Proveedor proveedor = buscarEntidad(id);
        proveedor.setNombre(cuerpo.nombre());
        proveedor.setNit(cuerpo.nit());
        proveedor.setContacto(cuerpo.contacto());
        proveedor.setTelefono(cuerpo.telefono());
        proveedor.setEmail(cuerpo.email());
        proveedor.setDireccion(cuerpo.direccion());
        return RespuestaProveedor.desde(proveedor);
    }

    @Override
    public RespuestaProveedor cambiarEstado(UUID id, EstadoProveedor valor) {
        Proveedor proveedor = buscarEntidad(id);
        proveedor.setEstado(valor);
        return RespuestaProveedor.desde(proveedor);
    }

    @Override
    public RespuestaProveedor calificar(UUID id, double valor) {
        if (valor < 0 || valor > 5) {
            throw new CalificacionInvalidaException(valor);
        }
        Proveedor proveedor = buscarEntidad(id);
        proveedor.setCalificacion(BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP));
        return RespuestaProveedor.desde(proveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaEntregaProveedor> entregas(UUID id) {
        buscarEntidad(id);
        return entregaRepository.findByProveedorIdOrderByFechaRecepcionDesc(id).stream()
                .map(RespuestaEntregaProveedor::desde)
                .toList();
    }

    private Proveedor buscarEntidad(UUID id) {
        return proveedorRepository.findById(id).orElseThrow(() -> new ProveedorNoEncontradoException(id));
    }
}
