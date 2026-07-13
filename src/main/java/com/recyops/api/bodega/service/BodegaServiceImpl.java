package com.recyops.api.bodega.service;

import com.recyops.api.bodega.dtos.CuerpoBodega;
import com.recyops.api.bodega.dtos.RespuestaBodega;
import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.enums.TipoOrganizacion;
import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.interfaces.BodegaService;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.dtos.RespuestaPagina;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BodegaServiceImpl implements BodegaService {

    private final BodegaRepository bodegaRepository;

    public BodegaServiceImpl(BodegaRepository bodegaRepository) {
        this.bodegaRepository = bodegaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaBodega> listar(EstadoBodega estado, TipoOrganizacion tipoOrganizacion,
            String nombre, int page, int size) {
        var pagina = bodegaRepository.buscar(estado, tipoOrganizacion, nombre, PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaBodega::desde);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaBodega obtener(UUID id) {
        return RespuestaBodega.desde(buscarEntidad(id));
    }

    @Override
    public RespuestaBodega crear(CuerpoBodega cuerpo) {
        Bodega bodega = Bodega.builder()
                .nombre(cuerpo.nombre())
                .direccion(cuerpo.direccion())
                .telefono(cuerpo.telefono())
                .email(cuerpo.email())
                .nit(cuerpo.nit())
                .latitud(cuerpo.latitud())
                .longitud(cuerpo.longitud())
                .tipoOrganizacion(cuerpo.tipoOrganizacion())
                .build();
        return RespuestaBodega.desde(bodegaRepository.save(bodega));
    }

    @Override
    public RespuestaBodega actualizar(UUID id, CuerpoBodega cuerpo) {
        Bodega bodega = buscarEntidad(id);
        bodega.setNombre(cuerpo.nombre());
        bodega.setDireccion(cuerpo.direccion());
        bodega.setTelefono(cuerpo.telefono());
        bodega.setEmail(cuerpo.email());
        bodega.setNit(cuerpo.nit());
        bodega.setLatitud(cuerpo.latitud());
        bodega.setLongitud(cuerpo.longitud());
        bodega.setTipoOrganizacion(cuerpo.tipoOrganizacion());
        return RespuestaBodega.desde(bodega);
    }

    @Override
    public RespuestaBodega cambiarEstado(UUID id, EstadoBodega valor) {
        Bodega bodega = buscarEntidad(id);
        bodega.setEstado(valor);
        return RespuestaBodega.desde(bodega);
    }

    private Bodega buscarEntidad(UUID id) {
        return bodegaRepository.findById(id).orElseThrow(() -> new BodegaNoEncontradaException(id));
    }
}
