package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
    public void setEmail(String email) { this.email = utn.simulacro_nombreAlumno.security.EmailNormalizer.normalize(email); }
}
