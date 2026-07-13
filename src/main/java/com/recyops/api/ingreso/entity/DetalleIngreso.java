package com.recyops.api.ingreso.entity;

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

/** Un material dentro de un ingreso: pesos de bascula y liquidacion por kilo. */
@Entity
@Table(name = "detalles_ingreso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingreso_id")
    private IngresoMaterial ingreso;

    @Column(nullable = false)
    private String categoria;

    @Column(name = "peso_bruto", nullable = false, precision = 14, scale = 2)
    private BigDecimal pesoBruto;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal tara = BigDecimal.ZERO;

    @Column(name = "peso_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal pesoNeto;

    @Column(name = "precio_kilo", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioKilo;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal subtotal;

    @Column(length = 500)
    private String observaciones;
}
