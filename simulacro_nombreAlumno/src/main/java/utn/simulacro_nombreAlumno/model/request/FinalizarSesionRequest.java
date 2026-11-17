package utn.simulacro_nombreAlumno.model.request;
import lombok.*;
import jakarta.validation.constraints.Size;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FinalizarSesionRequest { @Size(max=2000) private String comentario; }
