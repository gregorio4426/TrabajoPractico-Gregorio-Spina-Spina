package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

/** Referencia sin alumnos, rutinas, usuario ni credenciales: evita ciclos. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesorResumenResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String especialidad;
}
