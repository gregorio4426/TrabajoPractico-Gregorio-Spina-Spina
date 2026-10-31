package utn.simulacro_nombreAlumno.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaEjercicioResponse {

    private Long id;
    private String ejercicioNombre;
    private String grupoMuscular;
    private Integer seriesSugeridas;
    private Integer repsSugeridas;
    private Double pesoSugerido;

    private Long ejercicioId;

    // Conserva el constructor anterior para consumidores Java existentes.
    public DiaEjercicioResponse(Long id, String ejercicioNombre, String grupoMuscular, Integer seriesSugeridas, Integer repsSugeridas, Double pesoSugerido) {
        this.id = id;
        this.ejercicioNombre = ejercicioNombre;
        this.grupoMuscular = grupoMuscular;
        this.seriesSugeridas = seriesSugeridas;
        this.repsSugeridas = repsSugeridas;
        this.pesoSugerido = pesoSugerido;
    }
}