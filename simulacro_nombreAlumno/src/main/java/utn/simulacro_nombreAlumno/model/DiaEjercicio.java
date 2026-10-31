package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@org.hibernate.annotations.Immutable
@Table(name = "dias_ejercicio")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DiaEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dia_rutina_id", updatable = false)
    private DiaRutina diaRutina;

    @ManyToOne
    @JoinColumn(name = "ejercicio_id", updatable = false)
    private Ejercicio ejercicio;

    @Column(nullable = false)
    private Integer seriesSugeridas;

    @Column(nullable = false)
    private Integer repsSugeridas;

    private Double pesoSugerido;

    @Column(updatable = false, length = 60)
    private String ejercicioNombreSnapshot;
    @Column(updatable = false)
    private String ejercicioDescripcionSnapshot;
    @Column(updatable = false, length = 32)
    private String grupoMuscularSnapshot;

    @PrePersist
    void capturarEjercicio() {
        if (ejercicio != null && ejercicioNombreSnapshot == null) {
            ejercicioNombreSnapshot = ejercicio.getNombre();
            ejercicioDescripcionSnapshot = ejercicio.getDescripcion();
            grupoMuscularSnapshot = ejercicio.getGrupoMuscular() == null ? null : ejercicio.getGrupoMuscular().name();
        }
    }

    // Fallback solo para objetos anteriores a la migración y tests no persistidos.
    public String getNombreHistorico() {
        return ejercicioNombreSnapshot != null ? ejercicioNombreSnapshot : ejercicio == null ? null : ejercicio.getNombre();
    }
    public String getGrupoHistorico() {
        return ejercicioNombreSnapshot != null ? grupoMuscularSnapshot : ejercicio == null || ejercicio.getGrupoMuscular() == null
                ? null : ejercicio.getGrupoMuscular().name();
    }
}
