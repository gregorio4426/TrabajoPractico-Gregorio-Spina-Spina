package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionRutinaRequest {

    @NotNull
    private Long alumnoId;

    @NotNull
    private Long rutinaId;

}
