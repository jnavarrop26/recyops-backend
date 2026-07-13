package com.recyops.api.material.service;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.material.dtos.CuerpoMaterial;
import com.recyops.api.material.dtos.RespuestaMaterial;
import com.recyops.api.material.dtos.RespuestaOpcionCatalogo;
import com.recyops.api.material.entity.Material;
import com.recyops.api.material.entity.OpcionCatalogo;
import com.recyops.api.material.enums.TipoOpcionCatalogo;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.excepciones.OpcionCatalogoNoEncontradaException;
import com.recyops.api.material.interfaces.MaterialService;
import com.recyops.api.material.repository.MaterialRepository;
import com.recyops.api.material.repository.OpcionCatalogoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final OpcionCatalogoRepository catalogoRepository;

    public MaterialServiceImpl(MaterialRepository materialRepository,
            OpcionCatalogoRepository catalogoRepository) {
        this.materialRepository = materialRepository;
        this.catalogoRepository = catalogoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaMaterial> listar(String categoria, String resina, String color,
            String empaque, Boolean activo, int page, int size) {
        var pagina = materialRepository.buscar(categoria, resina, color, empaque, activo,
                PageRequest.of(page, size));
        return RespuestaPagina.desde(pagina, RespuestaMaterial::desde);
    }

    @Override
    public RespuestaMaterial crear(CuerpoMaterial cuerpo) {
        Material material = Material.builder().build();
        aplicarCuerpo(material, cuerpo);
        return RespuestaMaterial.desde(materialRepository.save(material));
    }

    @Override
    public RespuestaMaterial actualizar(UUID id, CuerpoMaterial cuerpo) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new MaterialNoEncontradoException(id));
        aplicarCuerpo(material, cuerpo);
        return RespuestaMaterial.desde(material);
    }

    @Override
    public RespuestaMaterial cambiarActivo(UUID id, boolean activo) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new MaterialNoEncontradoException(id));
        material.setActivo(activo);
        return RespuestaMaterial.desde(material);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaOpcionCatalogo> listarCategorias() {
        return listarOpciones(TipoOpcionCatalogo.CATEGORIA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaOpcionCatalogo> listarSubcategorias(String categoria) {
        return catalogoRepository
                .findByTipoAndCategoriaPadreCodigoOrderByNombre(TipoOpcionCatalogo.SUBCATEGORIA, categoria)
                .stream().map(RespuestaOpcionCatalogo::desde).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaOpcionCatalogo> listarResinas() {
        return listarOpciones(TipoOpcionCatalogo.RESINA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaOpcionCatalogo> listarColores() {
        return listarOpciones(TipoOpcionCatalogo.COLOR);
    }

    private List<RespuestaOpcionCatalogo> listarOpciones(TipoOpcionCatalogo tipo) {
        return catalogoRepository.findByTipoOrderByNombre(tipo).stream()
                .map(RespuestaOpcionCatalogo::desde).toList();
    }

    private void aplicarCuerpo(Material material, CuerpoMaterial cuerpo) {
        material.setNombre(cuerpo.nombre());
        material.setCategoria(resolver(TipoOpcionCatalogo.CATEGORIA, cuerpo.categoriaCodigo()));
        material.setSubcategoria(resolverOpcional(TipoOpcionCatalogo.SUBCATEGORIA, cuerpo.subcategoriaCodigo()));
        material.setResina(resolverOpcional(TipoOpcionCatalogo.RESINA, cuerpo.codigoResinaCodigo()));
        material.setColor(resolverOpcional(TipoOpcionCatalogo.COLOR, cuerpo.colorCodigo()));
        material.setUnidadMedida(cuerpo.unidadMedida());
        material.setUnidadEmpaque(cuerpo.unidadEmpaque());
        material.setPrecioBase(cuerpo.precioBase());
        material.setFactorCalidad(cuerpo.factorCalidad());
        material.setUmbralMerma(cuerpo.umbralMerma());
    }

    private OpcionCatalogo resolver(TipoOpcionCatalogo tipo, String codigo) {
        return catalogoRepository.findByTipoAndCodigo(tipo, codigo)
                .orElseThrow(() -> new OpcionCatalogoNoEncontradaException(tipo, codigo));
    }

    private OpcionCatalogo resolverOpcional(TipoOpcionCatalogo tipo, String codigo) {
        return codigo == null || codigo.isBlank() ? null : resolver(tipo, codigo);
    }
}
