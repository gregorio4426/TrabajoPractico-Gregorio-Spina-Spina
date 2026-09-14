package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaRutinaRequest {

    @NotBlank
    private String nombre;

    @NotNull
    private Integer numeroOrden;

    private List<DiaEjercicioRequest> ejercicios;
}