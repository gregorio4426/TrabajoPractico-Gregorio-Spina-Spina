package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import utn.simulacro_nombreAlumno.model.Nivel;
import utn.simulacro_nombreAlumno.model.Objetivo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguimientoAlumnoRequest {
    @NotNull
    private Nivel nivel;
    @NotNull
    private Objetivo objetivo;
}
