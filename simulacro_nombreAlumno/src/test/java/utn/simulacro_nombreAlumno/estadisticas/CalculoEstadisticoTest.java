package utn.simulacro_nombreAlumno.estadisticas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class CalculoEstadisticoTest {
    private final LocalDate fecha=LocalDate.of(2026,10,20);
    private RegistroEstadistico fila(long sesion,long serie,Double peso,int reps,boolean cerrada){
        return new RegistroEstadistico(sesion,1L,fecha,fecha,cerrada,serie,1L,"Press original",1,reps,peso,true,false,false,true);
    }
    @Test void emptyDataIsZeroCountsWithUnknownWeightAndVolume() {
        var m=new AcumuladorEntrenamiento().resultado();assertThat(m.sesionesRegistradas()).isZero();assertThat(m.pesoMaximo()).isNull();assertThat(m.volumenRegistrado()).isNull();assertThat(m.volumenCompleto()).isFalse();
    }
    @Test void unknownAndZeroWeightAreDifferentAndMissingWeightDoesNotEraseRepetitions() {
        var a=new AcumuladorEntrenamiento();a.agregar(fila(1,1,null,8,true));a.agregar(fila(1,2,0.0,10,true));var m=a.resultado();
        assertThat(m.entrenamientosConDatosValidos()).isEqualTo(1);assertThat(m.seriesValidas()).isEqualTo(2);assertThat(m.repeticionesTotales()).isEqualTo(18);
        assertThat(m.seriesConPeso()).isEqualTo(1);assertThat(m.seriesSinPeso()).isEqualTo(1);assertThat(m.pesoMaximo()).isEqualByComparingTo("0");assertThat(m.volumenRegistrado()).isEqualByComparingTo("0");assertThat(m.volumenCompleto()).isFalse();
    }
    @Test void decimalVolumeIsExactAndCannotOverflowDouble() {
        var a=new AcumuladorEntrenamiento();a.agregar(fila(1,1,0.1,3,true));a.agregar(fila(1,2,0.2,3,true));
        assertThat(a.resultado().volumenRegistrado()).isEqualByComparingTo("0.9");
        var b=new AcumuladorEntrenamiento();b.agregar(fila(1,1,Double.MAX_VALUE,Integer.MAX_VALUE,true));
        assertThat(b.resultado().volumenRegistrado()).isEqualByComparingTo(BigDecimal.valueOf(Double.MAX_VALUE).multiply(BigDecimal.valueOf(Integer.MAX_VALUE)));
    }
    @Test void closedLegacyEmptySessionsAndOpenSetsAreCountedWithoutInflatingProgress() {
        var a=new AcumuladorEntrenamiento();a.agregar(fila(1,1,60.0,10,false));
        a.agregar(new RegistroEstadistico(2,1,fecha,fecha,true,null,null,null,null,null,null,false,false,false,false));var m=a.resultado();
        assertThat(m.sesionesRegistradas()).isEqualTo(2);assertThat(m.sesionesAbiertas()).isEqualTo(1);assertThat(m.sesionesFinalizadas()).isEqualTo(1);
        assertThat(m.sesionesFinalizadasSinDatosValidos()).isEqualTo(1);assertThat(m.entrenamientosConDatosValidos()).isZero();assertThat(m.seriesEnSesionesAbiertas()).isEqualTo(1);assertThat(m.repeticionesTotales()).isZero();
    }
    @Test void invalidAnnuledExemptIncompleteOrMismatchingIdentitiesNeverContribute() {
        var a=new AcumuladorEntrenamiento();
        a.agregar(new RegistroEstadistico(1,1,fecha,fecha,true,1L,1L,"Press",1,8,60.0,true,true,false,true));
        a.agregar(new RegistroEstadistico(1,1,fecha,fecha,true,2L,1L,"Press",1,8,60.0,true,false,true,true));
        a.agregar(new RegistroEstadistico(1,1,fecha,fecha,true,3L,1L,"Press",1,8,60.0,false,false,false,true));
        a.agregar(new RegistroEstadistico(1,1,fecha,fecha,true,4L,1L,"Press",1,8,60.0,true,false,false,false));
        a.agregar(fila(1,5,-1.0,8,true));a.agregar(fila(1,6,Double.NaN,8,true));a.agregar(fila(1,7,Double.POSITIVE_INFINITY,8,true));a.agregar(fila(1,8,60.0,0,true));
        var m=a.resultado();assertThat(m.seriesExcluidas()).isEqualTo(8);assertThat(m.seriesValidas()).isZero();assertThat(m.volumenRegistrado()).isNull();
    }
    @Test void multipleSessionsSameDayProduceOneTrainingDayAndStableRepeatableResults() {
        var a=new AcumuladorEntrenamiento();a.agregar(fila(1,1,50.0,8,true));a.agregar(fila(2,2,55.0,10,true));var m=a.resultado();
        assertThat(m.entrenamientosConDatosValidos()).isEqualTo(2);assertThat(m.diasConEntrenamiento()).isEqualTo(1);assertThat(m.alumnosConEntrenamiento()).isEqualTo(1);assertThat(a.resultado()).isEqualTo(m);
        var f=FrecuenciaEstadistica.calcular(m,new EstadisticaDtos.Periodo(fecha.minusDays(13),fecha,14,"UTC"));
        assertThat(f.entrenamientosPorSemana()).isEqualByComparingTo("1");assertThat(f.diasEntrenadosPorSemana()).isEqualByComparingTo("0.5");
    }
    @Test void defaultWindowIsInclusiveAndUsesInjectedClock() {
        var p=PeriodoEstadistico.resolver(new EstadisticaFiltro(),Clock.fixed(Instant.parse("2026-10-30T00:01:00Z"),ZoneOffset.UTC));
        assertThat(p.desde()).isEqualTo(LocalDate.of(2026,10,1));assertThat(p.hasta()).isEqualTo(LocalDate.of(2026,10,30));assertThat(p.dias()).isEqualTo(30);
    }
    @Test void explicitEndDeterminesDefaultStartAndLeapYearWindowIsAccepted() {
        var f=new EstadisticaFiltro();f.setHasta(LocalDate.of(2024,12,31));var clock=Clock.systemUTC();
        assertThat(PeriodoEstadistico.resolver(f,clock).desde()).isEqualTo(LocalDate.of(2024,12,2));
        f.setDesde(LocalDate.of(2024,1,1));assertThat(PeriodoEstadistico.resolver(f,clock).dias()).isEqualTo(366);
    }
    @ParameterizedTest @ValueSource(ints={-1,367,10000})
    void invalidWindowIsRejected(int days) {
        var f=new EstadisticaFiltro();f.setHasta(fecha);f.setDesde(fecha.minusDays(days-1));
        assertThatThrownBy(()->PeriodoEstadistico.resolver(f,Clock.systemUTC())).isInstanceOf(utn.simulacro_nombreAlumno.exception.ReglaNegocioException.class);
    }
}
