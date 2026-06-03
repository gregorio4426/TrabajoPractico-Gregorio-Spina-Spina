package utn.simulacro_nombreAlumno.model.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;
import utn.simulacro_nombreAlumno.model.GrupoMuscular;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjercicioRequest {
    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;

    @NotBlank
    private GrupoMuscular grupoMuscular;



}

