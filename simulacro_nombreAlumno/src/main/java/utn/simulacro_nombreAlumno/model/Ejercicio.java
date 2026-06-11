package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ejercicios")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Ejercicio {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(nullable = false, length = 60)
	private String nombre;
    @Column(nullable = false)
	private String descripcion;

    @Enumerated(EnumType.STRING)
	private GrupoMuscular grupoMuscular;
}
