package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionResponse {
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activa;
    private String rutinaNombre;
    private String alumnoNombre;

    private Long id;
    private Long rutinaId;
    private Long alumnoId;
    private Long profesorId;

    // Conserva el constructor anterior para consumidores Java existentes.
    public AsignacionResponse(LocalDateTime fechaInicio, LocalDateTime fechaFin, boolean activa, String rutinaNombre, String alumnoNombre) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activa = activa;
        this.rutinaNombre = rutinaNombre;
        this.alumnoNombre = alumnoNombre;
    }
    private Long rutinaVersionId;
    private Integer numeroVersion;
    private boolean archivada;
}
