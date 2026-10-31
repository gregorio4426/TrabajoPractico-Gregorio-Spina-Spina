package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sesiones")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alumno_id", nullable = false, updatable = false)
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "dia_rutina_id", nullable = false, updatable = false)
    private DiaRutina diaRutina;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    private boolean completada;

    @OneToMany(mappedBy = "sesion", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("numeroSerie ASC, id ASC")
    @Builder.Default
    private List<Serie> series = new ArrayList<>();

    /** Nullable únicamente para sesiones históricas sin atribución demostrable. */
    @ManyToOne
    @JoinColumn(name = "asignacion_id", updatable = false)
    private AsignacionRutina asignacion;

    @Column(nullable = false, updatable = false, length = 40)
    @Builder.Default
    private String estadoVinculoAsignacion = "LEGADO_SIN_EVALUAR";

    private LocalDate fechaCorregida;
    private java.time.LocalDateTime finalizadaEn;
    private java.time.LocalDateTime actualizadaEn;
    @Version
    private long revision;
    public LocalDate getFechaEfectiva() { return fechaCorregida == null ? fecha : fechaCorregida; }
}
