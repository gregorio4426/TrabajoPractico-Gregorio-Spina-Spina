package utn.simulacro_nombreAlumno.estadisticas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import static utn.simulacro_nombreAlumno.estadisticas.EstadisticaDtos.*;
final class FrecuenciaEstadistica {
    private FrecuenciaEstadistica() {}
    static Frecuencia calcular(Metricas datos,Periodo p) {
        var dias=BigDecimal.valueOf(p.dias());
        return new Frecuencia(BigDecimal.valueOf(datos.entrenamientosConDatosValidos()).multiply(BigDecimal.valueOf(7)).divide(dias,4,RoundingMode.HALF_UP),
                BigDecimal.valueOf(datos.diasConEntrenamiento()).multiply(BigDecimal.valueOf(7)).divide(dias,4,RoundingMode.HALF_UP));
    }
}
