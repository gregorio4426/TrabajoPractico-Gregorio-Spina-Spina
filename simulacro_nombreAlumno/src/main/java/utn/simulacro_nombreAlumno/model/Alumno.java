package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "alumnos")
public class Alumno {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(nullable = false, length = 20)
	private String nombre;

    @Column(nullable = false, length = 20)
	private String apellido;

    @Column(nullable = false, length = 3)
    private int edad;

    @Column(nullable = false, unique = true, length = 150)
	private String email;

	private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private Nivel nivel;

    @Enumerated(EnumType.STRING)
    private Objetivo objetivo;

    @OneToOne
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @ManyToMany
    @JoinTable(
        name = "alumnos_profesore",
        joinColumns = @JoinColumn(name = "alumno_id"),
        inverseJoinColumns = @JoinColumn(name = "profesor_id"),
        uniqueConstraints = @UniqueConstraint(name="uk_alumno_profesor", columnNames={"alumno_id","profesor_id"})
    )
    @Builder.Default
    private List<Profesor> profesores = new ArrayList<>();

    @OneToMany(mappedBy = "alumno")
    private List<AsignacionRutina> rutinasAsignadas;

    @Column(length = 150)
    private String emailContacto;
}
