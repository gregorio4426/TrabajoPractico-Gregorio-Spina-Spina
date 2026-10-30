package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@org.hibernate.annotations.Immutable
@Table(name = "dias_rutina", indexes = @Index(name = "ix_dia_orden", columnList = "rutina_id,numero_orden,id"))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DiaRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Integer numeroOrden;

    @ManyToOne
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;

    @OneToMany(mappedBy = "diaRutina", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("id ASC")
    @Builder.Default
    private List<DiaEjercicio> ejercicios = new ArrayList<>();
}
