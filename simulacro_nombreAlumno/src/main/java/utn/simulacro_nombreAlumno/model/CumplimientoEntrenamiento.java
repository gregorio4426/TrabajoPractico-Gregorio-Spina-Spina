package utn.simulacro_nombreAlumno.model;

import java.util.*;

/** Cumplimiento del DÍA prescrito, nunca de todos los días de una rutina. */
public final class CumplimientoEntrenamiento {
    private CumplimientoEntrenamiento() {}
    public record Resultado(Boolean diaCumplido, long seriesEsperadas, long seriesRealizadas) {}
    public static Resultado evaluar(Sesion sesion) {
        var vigentes = sesion.getSeries().stream().filter(Serie::isVigenteValida).toList();
        if (sesion.getDiaRutina() == null || sesion.getDiaRutina().getEjercicios().isEmpty()) return new Resultado(null, 0, vigentes.size());
        long esperadas = 0;
        boolean completo = true;
        Map<Long, List<Serie>> porEjercicio = new HashMap<>();
        for (Serie serie : vigentes) {
            if (serie.getDiaEjercicio() != null && serie.getDiaEjercicio().getEjercicio() != null) {
                porEjercicio.computeIfAbsent(serie.getDiaEjercicio().getEjercicio().getId(), k -> new ArrayList<>()).add(serie);
            }
        }
        for (DiaEjercicio plan : sesion.getDiaRutina().getEjercicios()) {
            if (plan.getSeriesSugeridas() == null || plan.getSeriesSugeridas() <= 0 || plan.getRepsSugeridas() == null || plan.getRepsSugeridas() <= 0) {
                return new Resultado(null, 0, vigentes.size());
            }
            esperadas += plan.getSeriesSugeridas();
            long hechas = vigentes.stream().filter(s -> s.getDiaEjercicio() != null && Objects.equals(s.getDiaEjercicio().getId(), plan.getId()))
                    .filter(s -> s.getRepsHechas() >= plan.getRepsSugeridas()).count();
            completo &= hechas >= plan.getSeriesSugeridas();
        }
        // Numeración global por ejercicio, aunque aparezca en dos prescripciones del día.
        for (var group : porEjercicio.values()) {
            var numeros = group.stream().map(Serie::getNumeroSerie).sorted().toList();
            for (int i = 0; i < numeros.size(); i++) completo &= numeros.get(i) == i + 1;
        }
        return new Resultado(completo, esperadas, vigentes.size());
    }
}
