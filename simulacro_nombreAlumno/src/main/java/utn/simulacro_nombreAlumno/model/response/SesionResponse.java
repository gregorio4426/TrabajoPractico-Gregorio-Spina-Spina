package utn.simulacro_nombreAlumno.model.response;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionResponse {

    private Long id;
    private LocalDate fecha;
    private boolean completada;
    private String diaRutinaNombre;
    private String rutinaNombre;
    private List<SerieResponse> series;

    private Long alumnoId;
    private Long rutinaId;
    private Long diaRutinaId;

    // Conserva el constructor anterior para consumidores Java existentes.
    public SesionResponse(Long id, LocalDate fecha, boolean completada, String diaRutinaNombre, String rutinaNombre, List<SerieResponse> series) {
        this.id = id;
        this.fecha = fecha;
        this.completada = completada;
        this.diaRutinaNombre = diaRutinaNombre;
        this.rutinaNombre = rutinaNombre;
        this.series = series;
    }
    private Long rutinaVersionId;
    private Integer numeroVersion;
    private Long asignacionId;
    private String estadoVinculoAsignacion;
    private boolean finalizada;
    private Boolean diaCumplido;
    private long seriesEsperadas;
    private long seriesRealizadas;
    private long revision;
    private java.time.LocalDate fechaEfectiva;
    private java.time.LocalDateTime finalizadaEn;
}
