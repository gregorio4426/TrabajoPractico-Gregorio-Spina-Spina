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
public class    Rutina {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(nullable = false)
	private String nombre;
    @Column(nullable = false)
	private String descripcion;

    @OneToMany(mappedBy = "rutina", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiaRutina> dias = new ArrayList<>();

	@ManyToOne
	@JoinColumn(name = "profesor_id")
	private Profesor profesor;

    @OneToMany (mappedBy = "rutina")
    @Builder.Default
    private List<AsignacionRutina> asignaciones = new ArrayList<>();
}
