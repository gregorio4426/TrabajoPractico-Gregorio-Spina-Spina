package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
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
    @JoinColumn(name = "dia_rutina_id")
    private DiaRutina diaRutina;

    @ManyToOne
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;

    @Column(nullable = false)
    private Integer seriesSugeridas;

    @Column(nullable = false)
    private Integer repsSugeridas;

    private Double pesoSugerido;
}