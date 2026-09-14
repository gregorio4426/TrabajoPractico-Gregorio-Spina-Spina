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
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "dia_rutina_id")
    private DiaRutina diaRutina;

    @Column(nullable = false)
    private LocalDate fecha;

    private boolean completada;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Serie> series = new ArrayList<>();
}