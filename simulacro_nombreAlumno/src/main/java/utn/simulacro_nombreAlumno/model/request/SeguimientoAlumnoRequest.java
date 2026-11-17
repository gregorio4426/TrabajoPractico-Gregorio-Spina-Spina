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
    private Objetivo objetivo;
    @jakarta.validation.constraints.Size(max=2000)
    private String objetivoPersonalizado;
    @jakarta.validation.constraints.AssertTrue(message="Informá un objetivo libre o una categoría")
    public boolean isObjetivoInformado() { return objetivo != null || (objetivoPersonalizado != null && !objetivoPersonalizado.isBlank()); }
}
