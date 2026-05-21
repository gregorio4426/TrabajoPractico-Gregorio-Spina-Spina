package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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

    @Column(nullable = false, length = 20)
	private String nombre;

    @Column(nullable = false, length = 20)
	private String apellido;

    @Column(nullable = false, unique = true, length = 150)
	private String email;

	private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
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
