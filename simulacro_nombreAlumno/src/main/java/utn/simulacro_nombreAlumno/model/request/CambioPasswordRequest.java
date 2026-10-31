package utn.simulacro_nombreAlumno.model.request;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CambioPasswordRequest {
    @NotBlank private String nuevaPassword;
    @NotBlank private String passwordActual;
}
