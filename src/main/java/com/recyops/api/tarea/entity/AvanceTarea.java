package com.recyops.api.tarea.entity;

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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Entrada de la bitacora de una tarea: que se hizo, cuanto (opcional),
 * quien y cuando. Es evidencia de trabajo: no se edita ni se borra suelta;
 * solo desaparece si el admin elimina la tarea completa en Revision.
 */
@Entity
@Table(name = "avances_tarea")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvanceTarea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tarea_id")
    private Tarea tarea;

    /** Cantidad producida/procesada; null cuando el avance no es cuantificable. */
    @Column(precision = 14, scale = 2)
    private BigDecimal cantidad;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(name = "usuario_nombre", nullable = false)
    private String usuarioNombre;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;
}
