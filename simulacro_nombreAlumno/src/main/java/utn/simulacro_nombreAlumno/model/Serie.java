package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "series")
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
    @JoinColumn(name = "sesion_id")
    private Sesion sesion;

    @ManyToOne
    @JoinColumn(name = "dia_ejercicio_id")
    private DiaEjercicio diaEjercicio;

    @Column(nullable = false)
    private Integer numeroSerie;

    private Integer repsHechas;

    private Double pesoUsado;

    private boolean completada;
}