package utn.simulacro_nombreAlumno.model.response;

import lombok.*;
import utn.simulacro_nombreAlumno.model.GrupoMuscular;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjercicioResponse {

    private String nombre;
    private String descripcion;


    private Long id;
    private GrupoMuscular grupoMuscular;
    private Long grupoId;
    private String grupoMuscularNombre;
    public EjercicioResponse(String nombre,String descripcion,Long id,GrupoMuscular grupo) {this.nombre=nombre;this.descripcion=descripcion;this.id=id;this.grupoMuscular=grupo;}

    // Conserva el constructor anterior para consumidores Java existentes.
    public EjercicioResponse(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}

