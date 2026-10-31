package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "asignaciones_rutina", uniqueConstraints = @UniqueConstraint(name = "uk_alumno_asignacion_activa", columnNames = "alumno_activo_id"))

public class AsignacionRutina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activa;

    @ManyToOne
    @JoinColumn(name = "rutina_id", nullable = false, updatable = false)
    private Rutina rutina;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false, updatable = false)
    private Alumno alumno;
    @Column(insertable = false, updatable = false, columnDefinition = "bigint GENERATED ALWAYS AS (CASE WHEN activa THEN alumno_id ELSE NULL END)")
    private Long alumnoActivoId;
}
