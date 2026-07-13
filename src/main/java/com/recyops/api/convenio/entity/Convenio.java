package com.recyops.api.convenio.entity;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import com.recyops.api.proveedor.entity.Proveedor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "convenios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convenio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Consecutivo legible, ej. CNV-000007 (secuencia convenios_codigo_seq). */
    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConvenio tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_id")
    private Bodega bodega;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "valor_total", precision = 16, scale = 2)
    private BigDecimal valorTotal;

    private String responsable;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoConvenio estado = EstadoConvenio.ACTIVO;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}
