package utn.simulacro_nombreAlumno.model.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutinaRequest {

    @NotBlank
    @Size(max = 255)
    private String nombre;

    @NotBlank
    @Size(max = 255)
    private String descripcion;

    @NotEmpty(groups = utn.simulacro_nombreAlumno.validation.CreacionRutina.class)
    private List<@NotNull @Valid DiaRutinaRequest> dias;
}

