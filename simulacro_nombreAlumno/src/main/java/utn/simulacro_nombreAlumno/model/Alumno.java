package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "alumnos")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Alumno {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;
	private String apellido;
	private String email;
	private LocalDate fechaNacimiento;
    private Nivel nivel;
    private Objetivo objetivo;

    @ManyToMany
    @JoinTable(
        name = "alumnos_profesore",
        joinColumns = @JoinColumn(name = "alumno_id"),
        inverseJoinColumns = @JoinColumn(name = "profesor_id")
    )
    private List<Profesor> profesores;

    @OneToMany(mappedBy = "alumno")
    private List<AsignacionRutina> rutinasAsignadas;

}
