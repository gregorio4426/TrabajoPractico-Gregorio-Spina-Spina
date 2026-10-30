package utn.simulacro_nombreAlumno.estadisticas;

import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import static utn.simulacro_nombreAlumno.estadisticas.EstadisticaDtos.*;

public final class PeriodoEstadistico {
    private PeriodoEstadistico() {}
    public static Periodo resolver(EstadisticaFiltro filtro,Clock clock) {
        LocalDate hasta=filtro.getHasta()==null?LocalDate.now(clock):filtro.getHasta();
        // Las fechas extremas no son útiles ni representables por el esquema MySQL DATE.
        if(hasta.isBefore(LocalDate.of(1000,1,30)) || hasta.isAfter(LocalDate.of(9999,12,31)))
            throw new ReglaNegocioException("Fecha fuera del rango soportado");
        LocalDate desde=filtro.getDesde()==null?hasta.minusDays(29):filtro.getDesde();
        long dias=ChronoUnit.DAYS.between(desde,hasta)+1;
        if(desde.isBefore(LocalDate.of(1000,1,1)) || dias<1 || dias>366)
            throw new ReglaNegocioException("El período debe contener entre 1 y 366 días inclusivos");
        return new Periodo(desde,hasta,dias,"UTC");
    }
}
