package com.recyops.api.inventario.entity;

import com.recyops.api.bodega.entity.Bodega;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/** Stock de un material dentro de una bodega, con topes minimo y maximo. */
@Entity
@Table(name = "lineas_inventario",
        uniqueConstraints = @UniqueConstraint(columnNames = { "bodega_id", "tipo_material_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bodega_id")
    private Bodega bodega;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_material_id")
    private Material tipoMaterial;

    @Builder.Default
    @Column(name = "stock_actual", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockMinimo;

    @Column(name = "stock_maximo", nullable = false, precision = 14, scale = 2)
    private BigDecimal stockMaximo;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /** Bloqueo optimista: entradas/salidas/ajustes/mermas concurrentes sobre
     * la misma linea no se pisan en silencio, la segunda falla con 409. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public boolean estaBajoMinimo() {
        return stockActual.compareTo(stockMinimo) < 0;
    }
}
