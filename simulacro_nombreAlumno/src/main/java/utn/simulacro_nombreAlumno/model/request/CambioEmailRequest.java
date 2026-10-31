package utn.simulacro_nombreAlumno.model.request;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CambioEmailRequest {
    @jakarta.validation.constraints.Email @NotBlank @Size(max=150) private String email;
    @NotBlank private String passwordActual;
    public void setEmail(String email) { this.email = utn.simulacro_nombreAlumno.security.EmailNormalizer.normalize(email); }
}
