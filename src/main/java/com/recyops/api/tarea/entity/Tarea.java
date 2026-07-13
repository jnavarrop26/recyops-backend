package com.recyops.api.tarea.entity;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.tarea.enums.EstadoTarea;
import com.recyops.api.tarea.enums.PrioridadTarea;
import com.recyops.api.usuario.entity.Usuario;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Tarea operativa asignada a un trabajador, opcionalmente ligada a una bodega. */
@Entity
@Table(name = "tareas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignado_id")
    private Usuario asignado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bodega_id")
    private Bodega bodega;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadTarea prioridad = PrioridadTarea.MEDIA;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "creado_por_nombre", nullable = false)
    private String creadoPorNombre;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;
}
