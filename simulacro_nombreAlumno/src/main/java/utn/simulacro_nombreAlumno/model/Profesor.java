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

    @Column(nullable = false, length = 20)
	private String nombre;

    @Column(nullable = false, length = 20)
	private String apellido;

    @Column(nullable = false,unique = true ,length = 150)
	private String email;

    @Column(nullable = false, length = 50)
	private String especialidad;

    @OneToOne
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @ManyToMany(mappedBy = "profesores")
    private List<Alumno> alumnos;

    @OneToMany (mappedBy = "profesor")
    private List <Rutina> rutinas;
}
