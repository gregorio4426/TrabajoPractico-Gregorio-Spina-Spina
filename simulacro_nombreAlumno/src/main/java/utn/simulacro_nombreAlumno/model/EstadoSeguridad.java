package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "seguridad_sistema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EstadoSeguridad { @Id private Long id; }
