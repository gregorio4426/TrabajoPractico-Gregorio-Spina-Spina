package utn.simulacro_nombreAlumno.estadisticas;

import java.time.LocalDate;

/** Proyección de solo lectura; no materializa entidades, contraseñas ni listas de series. */
record RegistroEstadistico(long sesionId,long alumnoId,LocalDate fechaOriginal,LocalDate fechaEfectiva,boolean finalizada,
        Long serieId,Long ejercicioId,String nombreHistorico,Integer numeroSerie,Integer repeticiones,Double peso,
        boolean completada,boolean anulada,boolean legadoExento,boolean identidadCoherente) {
    boolean serieValida() {
        return serieId!=null && identidadCoherente && !anulada && !legadoExento && completada
                && numeroSerie!=null && numeroSerie>0 && repeticiones!=null && repeticiones>0
                && (peso==null || Double.isFinite(peso) && peso>=0);
    }
}
