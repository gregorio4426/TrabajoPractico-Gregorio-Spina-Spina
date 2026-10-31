package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieResponse {

    private Long id;
    private String ejercicioNombre;
    private Integer numeroSerie;
    private Integer repsHechas;
    private Double pesoUsado;
    private boolean completada;

    private Long sesionId;
    private Long diaEjercicioId;
    private Long ejercicioId;

    // Conserva el constructor anterior para consumidores Java existentes.
    public SerieResponse(Long id, String ejercicioNombre, Integer numeroSerie, Integer repsHechas, Double pesoUsado, boolean completada) {
        this.id = id;
        this.ejercicioNombre = ejercicioNombre;
        this.numeroSerie = numeroSerie;
        this.repsHechas = repsHechas;
        this.pesoUsado = pesoUsado;
        this.completada = completada;
    }
    private java.time.LocalDate fecha;
    private boolean anulada;
    private boolean legadoExento;
    private String incidenciaLegado;
    private long revision;
    private java.time.LocalDate fechaEfectiva;
}