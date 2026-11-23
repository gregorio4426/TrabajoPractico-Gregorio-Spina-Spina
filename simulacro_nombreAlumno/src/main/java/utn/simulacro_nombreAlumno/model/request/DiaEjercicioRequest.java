package utn.simulacro_nombreAlumno.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaEjercicioRequest {

    @NotNull
    @Positive
    private Long ejercicioId;

    @NotNull
    @Positive
    private Integer seriesSugeridas;

    @Positive
    private Integer repsSugeridas;

    @PesoValido
    private Double pesoSugerido;
    @Positive private Integer bloqueOrden;
    @Size(max=100) private String bloqueNombre;
    @Positive private Integer vueltas;
    @Positive private Integer ordenEnBloque;
    @PositiveOrZero private Integer descansoEjercicioSegundos;
    @PositiveOrZero private Integer descansoVueltaSegundos;


    @Positive private Integer segundosSugeridos;
    @Builder.Default private utn.simulacro_nombreAlumno.model.TipoMedicion tipoMedicion = utn.simulacro_nombreAlumno.model.TipoMedicion.REPETICIONES;
}