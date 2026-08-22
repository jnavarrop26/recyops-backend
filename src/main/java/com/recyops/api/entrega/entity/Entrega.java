package com.recyops.api.entrega.entity;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.convenio.entity.Convenio;
import com.recyops.api.entrega.enums.EstadoEntrega;
import com.recyops.api.usuario.entity.Usuario;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entregas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Consecutivo legible, ej. ENT-000042 (secuencia entregas_codigo_seq). */
    @Column(nullable = false, unique = true)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convenio_id")
    private Convenio convenio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bodega_id")
    private Bodega bodega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_entrega_id")
    private Usuario personaEntrega;

    @Column(name = "total_kg", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalKg;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrega estado = EstadoEntrega.RECIBIDA;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;

    @Column(name = "usuario_registro_nombre", nullable = false)
    private String usuarioRegistroNombre;

    @Builder.Default
    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaEntrega> lineas = new ArrayList<>();
}
