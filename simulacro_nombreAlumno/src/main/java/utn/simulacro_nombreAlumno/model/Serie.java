package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "series", uniqueConstraints = @UniqueConstraint(name = "uk_serie_numero", columnNames = {"sesion_id", "ejercicio_id", "numero_serie_unica"}))
@org.hibernate.annotations.Check(name = "ck_serie_valores", constraints = "legado_exento OR (numero_serie > 0 AND reps_hechas IS NOT NULL AND reps_hechas > 0 AND (peso_usado IS NULL OR peso_usado >= 0))")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Serie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sesion_id", nullable = false, updatable = false)
    private Sesion sesion;

    @ManyToOne
    @JoinColumn(name = "dia_ejercicio_id", nullable = false, updatable = false)
    private DiaEjercicio diaEjercicio;

    @Column(nullable = false)
    private Integer numeroSerie;

    private Integer repsHechas;

    private Double pesoUsado;

    private boolean completada;

    @ManyToOne
    @JoinColumn(name = "ejercicio_id", nullable = false, updatable = false)
    private Ejercicio ejercicio;
    private boolean anulada;
    private boolean legadoExento;
    @Column(length = 128, updatable = false)
    private String incidenciaLegado;
    @Version
    private long revision;
    @Column(insertable = false, updatable = false, columnDefinition = "integer GENERATED ALWAYS AS (CASE WHEN NOT anulada AND NOT legado_exento THEN numero_serie ELSE NULL END)")
    private Integer numeroSerieUnica;

    @PrePersist
    void fijarEjercicio() {
        if (ejercicio == null && diaEjercicio != null) ejercicio = diaEjercicio.getEjercicio();
    }

    public boolean isVigenteValida() {
        return !anulada && !legadoExento && completada && numeroSerie != null && numeroSerie > 0
                && repsHechas != null && repsHechas > 0
                && (pesoUsado == null || (Double.isFinite(pesoUsado) && pesoUsado >= 0));
    }
}
