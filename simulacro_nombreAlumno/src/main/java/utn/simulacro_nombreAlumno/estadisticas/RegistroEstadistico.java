package utn.simulacro_nombreAlumno.estadisticas;

import java.time.LocalDate;

/** Proyección de solo lectura; no materializa entidades, contraseñas ni listas de series. */
record RegistroEstadistico(long sesionId,long alumnoId,LocalDate fechaOriginal,LocalDate fechaEfectiva,boolean finalizada,
        Long serieId,Long ejercicioId,String nombreHistorico,Integer numeroSerie,Integer repeticiones,Double peso,
        boolean completada,boolean anulada,boolean legadoExento,boolean identidadCoherente,String tipoMedicion,Integer segundos) {
    RegistroEstadistico(long sesionId,long alumnoId,LocalDate fechaOriginal,LocalDate fechaEfectiva,boolean finalizada,Long serieId,Long ejercicioId,String nombreHistorico,Integer numeroSerie,Integer repeticiones,Double peso,boolean completada,boolean anulada,boolean legadoExento,boolean identidadCoherente) {
        this(sesionId,alumnoId,fechaOriginal,fechaEfectiva,finalizada,serieId,ejercicioId,nombreHistorico,numeroSerie,repeticiones,peso,completada,anulada,legadoExento,identidadCoherente,"REPETICIONES",null);
    }
    boolean serieValida() {
        return serieId!=null && identidadCoherente && !anulada && !legadoExento && completada
                && numeroSerie!=null && numeroSerie>0 && ("SEGUNDOS".equals(tipoMedicion)?segundos!=null && segundos>0 && repeticiones==null:repeticiones!=null && repeticiones>0 && segundos==null)
                && (peso==null || Double.isFinite(peso) && peso>=0);
    }
}
