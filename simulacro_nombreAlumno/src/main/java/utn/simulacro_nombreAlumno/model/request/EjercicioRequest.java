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
    @NotBlank @Size(max=60)
    private String nombre;

    @Size(max=255)
    private String descripcion;

    private GrupoMuscular grupoMuscular;
    @Positive private Long grupoId;
    @AssertTrue(message="Seleccioná un grupo muscular")
    public boolean isGrupoInformado() { return grupoId != null || grupoMuscular != null; }
    public EjercicioRequest(String nombre,String descripcion,GrupoMuscular grupo) {this.nombre=nombre;this.descripcion=descripcion;this.grupoMuscular=grupo;}


}

