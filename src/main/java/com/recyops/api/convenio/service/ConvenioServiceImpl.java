package com.recyops.api.convenio.service;

import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.repository.BodegaRepository;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.convenio.dtos.CuerpoConvenio;
import com.recyops.api.convenio.dtos.RespuestaConvenio;
import com.recyops.api.convenio.entity.Convenio;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import com.recyops.api.convenio.excepciones.ConvenioNoEncontradoException;
import com.recyops.api.convenio.excepciones.FechasConvenioInvalidasException;
import com.recyops.api.convenio.interfaces.ConvenioService;
import com.recyops.api.convenio.repository.ConvenioRepository;
import com.recyops.api.proveedor.excepciones.ProveedorNoEncontradoException;
import com.recyops.api.proveedor.repository.ProveedorRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConvenioServiceImpl implements ConvenioService {

    private final ConvenioRepository convenioRepository;
    private final ProveedorRepository proveedorRepository;
    private final BodegaRepository bodegaRepository;

    public ConvenioServiceImpl(ConvenioRepository convenioRepository,
            ProveedorRepository proveedorRepository, BodegaRepository bodegaRepository) {
        this.convenioRepository = convenioRepository;
        this.proveedorRepository = proveedorRepository;
        this.bodegaRepository = bodegaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaConvenio> listar(EstadoConvenio estado, TipoConvenio tipo, String nombre,
            int page, int size) {
        var pagina = convenioRepository.buscar(estado, tipo, nombre, PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaConvenio::desde);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaConvenio obtener(UUID id) {
        return RespuestaConvenio.desde(buscarEntidad(id));
    }

    @Override
    public RespuestaConvenio crear(CuerpoConvenio cuerpo) {
        Convenio convenio = Convenio.builder()
                .codigo("CNV-%06d".formatted(convenioRepository.siguienteConsecutivo()))
                .build();
        aplicarCuerpo(convenio, cuerpo);
        return RespuestaConvenio.desde(convenioRepository.save(convenio));
    }

    @Override
    public RespuestaConvenio actualizar(UUID id, CuerpoConvenio cuerpo) {
        Convenio convenio = buscarEntidad(id);
        aplicarCuerpo(convenio, cuerpo);
        return RespuestaConvenio.desde(convenio);
    }

    @Override
    public RespuestaConvenio cambiarEstado(UUID id, EstadoConvenio valor) {
        Convenio convenio = buscarEntidad(id);
        convenio.setEstado(valor);
        return RespuestaConvenio.desde(convenio);
    }

    private void aplicarCuerpo(Convenio convenio, CuerpoConvenio cuerpo) {
        if (cuerpo.fechaFin() != null && cuerpo.fechaFin().isBefore(cuerpo.fechaInicio())) {
            throw new FechasConvenioInvalidasException();
        }
        convenio.setNombre(cuerpo.nombre());
        convenio.setTipo(cuerpo.tipo());
        convenio.setProveedor(cuerpo.proveedorId() == null ? null
                : proveedorRepository.findById(cuerpo.proveedorId())
                        .orElseThrow(() -> new ProveedorNoEncontradoException(cuerpo.proveedorId())));
        convenio.setBodega(cuerpo.bodegaId() == null ? null
                : bodegaRepository.findById(cuerpo.bodegaId())
                        .orElseThrow(() -> new BodegaNoEncontradaException(cuerpo.bodegaId())));
        convenio.setFechaInicio(cuerpo.fechaInicio());
        convenio.setFechaFin(cuerpo.fechaFin());
        convenio.setValorTotal(cuerpo.valorTotal());
        convenio.setResponsable(cuerpo.responsable());
        convenio.setDescripcion(cuerpo.descripcion());
    }

    private Convenio buscarEntidad(UUID id) {
        return convenioRepository.findById(id).orElseThrow(() -> new ConvenioNoEncontradoException(id));
    }
}
