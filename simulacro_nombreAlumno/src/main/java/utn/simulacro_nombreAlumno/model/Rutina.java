package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
	private String nombre;
    @Column(nullable = false)
	private String descripcion;

	@ManyToMany
	@JoinTable(
			name = "rutina_ejercicio",
			joinColumns = @JoinColumn(name = "rutina_id"),
			inverseJoinColumns = @JoinColumn(name = "ejercicio_id")
	)
	private List<Ejercicio> ejercicios;

	@ManyToOne
	@JoinColumn(name = "profesor_id")
	private Profesor profesor;

    @OneToMany (mappedBy = "rutina")
    @Builder.Default
    private List<AsignacionRutina> asignaciones = new ArrayList<>();
}
