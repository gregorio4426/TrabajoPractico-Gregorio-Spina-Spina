package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "rutinas")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Rutina {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;
	private String descripcion;

	@ManyToMany
	@JoinTable(
			name = "rutina_ejercicio",
			joinColumns = @JoinColumn(name = "rutina_id"),
			inverseJoinColumns = @JoinColumn(name = "ejercicio_id")
	)
	private Set<Ejercicio> ejercicios;

	@ManyToOne
	@JoinColumn(name = "profesor_id")
	private Profesor profesor;

	@OneToMany (mappedBy = "rutina")
    private List<Alumno> alumnos;

    @OneToMany (mappedBy = "rutina")
    private List<AsignacionRutina> asignaciones;
}
