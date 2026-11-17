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
    @ManyToOne @JoinColumn(name="grupo_muscular_id")
    private GrupoMuscularCatalogo grupoPersonalizado;
    @Column(length=60,unique=true) private String nombreNormalizado;
    public String getGrupoNombre() { return grupoPersonalizado != null ? grupoPersonalizado.getNombre() : grupoMuscular == null ? null : grupoMuscular.name(); }

}
