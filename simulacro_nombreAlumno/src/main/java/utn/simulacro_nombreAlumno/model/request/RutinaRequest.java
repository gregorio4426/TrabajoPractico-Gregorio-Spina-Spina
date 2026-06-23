package utn.simulacro_nombreAlumno.model.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutinaRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;

    private List<Long> ejercicioIds;
}

