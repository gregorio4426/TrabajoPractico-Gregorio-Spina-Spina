package utn.simulacro_nombreAlumno.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="grupos_musculares") @Getter @Setter @NoArgsConstructor
public class GrupoMuscularCatalogo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=100) private String nombre;
 @Column(nullable=false,length=100,unique=true) private String normalizado;
}
