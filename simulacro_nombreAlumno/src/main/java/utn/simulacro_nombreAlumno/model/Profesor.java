package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "profesores")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Profesor {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;
	private String apellido;
	private String email;
	private String especialidad;

    @ManyToMany(mappedBy = "profesores")
    private List<Alumno> alumnos;

    @OneToMany (mappedBy = "profesor")
    private List <Rutina> rutinas;
}
