package com.recyops.api.entrega.entity;

import com.recyops.api.material.entity.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un material dentro de una entrega: cuanto peso llego de ese material. */
@Entity
@Table(name = "detalle_entregas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrega_id")
    private Entrega entrega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_material_id")
    private Material tipoMaterial;

    @Column(name = "peso_kg", nullable = false, precision = 14, scale = 2)
    private BigDecimal pesoKg;
}
