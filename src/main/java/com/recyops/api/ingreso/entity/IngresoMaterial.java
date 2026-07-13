package com.recyops.api.ingreso.entity;

import com.recyops.api.ingreso.enums.EstadoIngreso;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.hibernate.annotations.CreationTimestamp;

/**
 * Ingreso de material a bascula: quien lo trae, a que bodega va
 * y los totales de peso y valor. El cliente lo consulta como historial.
 */
@Entity
@Table(name = "ingresos_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngresoMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Codigo universal del recibo: relaciona el folio legible (REC-000042,
     * derivado del id) con un identificador global no adivinable.
     */
    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private String cliente;

    @Column(nullable = false)
    private String cedula;

    @Column(name = "bodega_destino", nullable = false)
    private String bodegaDestino;

    @Column(nullable = false)
    private String encargado;

    @Column(name = "placa_vehiculo")
    private String placaVehiculo;

    @Column(name = "peso_neto_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal pesoNetoTotal;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal total;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIngreso estado = EstadoIngreso.POR_CLASIFICAR;

    @Builder.Default
    @OneToMany(mappedBy = "ingreso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleIngreso> detalles = new ArrayList<>();
}
