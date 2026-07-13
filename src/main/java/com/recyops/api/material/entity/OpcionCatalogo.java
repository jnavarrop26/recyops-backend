package com.recyops.api.material.entity;

import com.recyops.api.material.enums.TipoOpcionCatalogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Opcion de catalogo (categoria, subcategoria, resina o color).
 * Las subcategorias referencian el codigo de su categoria padre.
 */
@Entity
@Table(name = "opciones_catalogo", uniqueConstraints = @UniqueConstraint(columnNames = { "tipo", "codigo" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOpcionCatalogo tipo;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    /** Solo aplica a resinas (frontend: esTermoplastico). */
    @Column(name = "es_termoplastico")
    private Boolean esTermoplastico;

    /** Codigo de la categoria padre; solo aplica a subcategorias. */
    @Column(name = "categoria_padre_codigo")
    private String categoriaPadreCodigo;
}
