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

	private String nombre;
	private String descripcion;
	private GrupoMuscular grupoMuscular;
}
