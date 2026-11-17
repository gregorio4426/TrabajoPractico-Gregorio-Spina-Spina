package utn.simulacro_nombreAlumno.model.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import utn.simulacro_nombreAlumno.validation.PesoValido;
import java.util.List;
public record PesosAsignacionRequest(@NotNull List<@NotNull @Valid Peso> pesos, @PositiveOrZero Long revision) {
 public record Peso(@NotNull @Positive Long diaEjercicioId, @PesoValido Double pesoSugerido) {}
}
