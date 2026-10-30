package utn.simulacro_nombreAlumno.model.response;

import lombok.*;
import java.time.LocalDateTime;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutinaResponse {

    private String nombre;
    private String descripcion;
    private List<DiaRutinaResponse> dias;
    private String profesor;
    private List<String> alumnos;

    private Long id;
    private Long profesorId;
    private Long asignacionId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Boolean activa;

    // Conserva el constructor anterior para consumidores Java existentes.
    public RutinaResponse(String nombre, String descripcion, List<DiaRutinaResponse> dias, String profesor, List<String> alumnos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.dias = dias;
        this.profesor = profesor;
        this.alumnos = alumnos;
    }
    private Long rutinaVersionId;
    private Integer numeroVersion;
    private boolean archivada;
    private String origenVersion;
    private java.time.LocalDateTime capturadaEn;
}

