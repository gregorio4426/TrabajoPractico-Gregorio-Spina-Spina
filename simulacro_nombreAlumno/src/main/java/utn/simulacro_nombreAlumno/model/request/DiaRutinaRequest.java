package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaRutinaRequest {

    @NotBlank
    @Size(max = 255)
    private String nombre;

    @NotNull
    @Positive
    private Integer numeroOrden;

    @NotEmpty
    private List<@NotNull @Valid DiaEjercicioRequest> ejercicios;
}