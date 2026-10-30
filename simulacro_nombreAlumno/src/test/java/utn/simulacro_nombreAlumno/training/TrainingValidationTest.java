package utn.simulacro_nombreAlumno.training;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.*;
import utn.simulacro_nombreAlumno.validation.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class TrainingValidationTest {
    private static ValidatorFactory factory;
    private ValidacionEntrenamiento validacion;
    @BeforeAll static void createValidator() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidator() { factory.close(); }
    @BeforeEach void setup() { validacion = new ValidacionEntrenamiento(factory.getValidator()); }

    static Stream<Double> invalidWeights() { return Stream.of(-0.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY); }
    @ParameterizedTest @MethodSource("invalidWeights")
    void rejectsNonFiniteAndNegativeWeights(Double weight) {
        var request = SerieRequest.builder().diaEjercicioId(1L).numeroSerie(1).repsHechas(8).pesoUsado(weight).build();
        assertThatThrownBy(() -> validacion.validar(request)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
    @ParameterizedTest @NullSource @ValueSource(doubles = {0, 52.5})
    void acceptsUnknownZeroAndPositiveWeights(Double weight) {
        assertThatCode(() -> validacion.validar(SerieRequest.builder().diaEjercicioId(1L).numeroSerie(1).repsHechas(8).pesoUsado(weight).build())).doesNotThrowAnyException();
    }
    @ParameterizedTest @NullSource @ValueSource(ints = {-1, 0})
    void repetitionsAreRequiredAndPositive(Integer reps) {
        assertThatThrownBy(() -> validacion.validar(SerieRequest.builder().diaEjercicioId(1L).numeroSerie(1).repsHechas(reps).build()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
    @ParameterizedTest @NullSource @ValueSource(ints = {-1, 0})
    void seriesNumbersAreRequiredAndPositive(Integer number) {
        assertThatThrownBy(() -> validacion.validar(SerieRequest.builder().diaEjercicioId(1L).numeroSerie(number).repsHechas(8).build()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
    @Test void nestedValidationRejectsMissingDaysExercisesAndInvalidPrescription() {
        var plan = RutinaRequest.builder().nombre("Plan").descripcion("Texto").build();
        assertThatThrownBy(() -> validacion.rutina(plan, true)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatCode(() -> validacion.rutina(plan, false)).doesNotThrowAnyException(); // PUT parcial compatible.
        plan.setDias(List.of(DiaRutinaRequest.builder().nombre("Día").numeroOrden(1).ejercicios(List.of()).build()));
        assertThatThrownBy(() -> validacion.rutina(plan, true)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        plan.getDias().getFirst().setEjercicios(List.of(DiaEjercicioRequest.builder().ejercicioId(1L).seriesSugeridas(0).repsSugeridas(8).build()));
        assertThatThrownBy(() -> validacion.rutina(plan, true)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        plan.getDias().getFirst().getEjercicios().getFirst().setSeriesSugeridas(1);
        plan.setDias(Arrays.asList(plan.getDias().getFirst(), null));
        assertThatThrownBy(() -> validacion.rutina(plan, true)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
    @Test void duplicateDayOrdersAreRejectedAtServiceValidation() {
        var exercise = DiaEjercicioRequest.builder().ejercicioId(1L).seriesSugeridas(1).repsSugeridas(8).build();
        var day = DiaRutinaRequest.builder().nombre("Día").numeroOrden(1).ejercicios(List.of(exercise)).build();
        assertThatThrownBy(() -> validacion.rutina(RutinaRequest.builder().nombre("Plan").descripcion("Texto").dias(List.of(day, day)).build(), true))
                .isInstanceOf(utn.simulacro_nombreAlumno.exception.ReglaNegocioException.class);
    }
    @Test void correctionsRequireReasonAndNonnegativeExpectedRevision() {
        assertThatThrownBy(() -> validacion.validar(CorreccionSerieRequest.builder().numeroSerie(1).repsHechas(8).motivo(" ").revision(-1L).build()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatThrownBy(() -> validacion.validar(CorreccionSesionRequest.builder().fecha(java.time.LocalDate.now().plusDays(1)).motivo("Error de fecha").revision(0L).build()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
    @Test void finishedPartialAndCompliantDayAreDifferentStates() {
        var exercise = Ejercicio.builder().id(1L).build();
        var plan = DiaEjercicio.builder().id(10L).ejercicio(exercise).seriesSugeridas(2).repsSugeridas(8).build();
        var day = DiaRutina.builder().ejercicios(List.of(plan)).build();
        var session = Sesion.builder().completada(true).diaRutina(day).build();
        var first = Serie.builder().id(1L).diaEjercicio(plan).numeroSerie(1).repsHechas(8).completada(true).build();
        session.setSeries(new ArrayList<>(List.of(first)));
        assertThat(CumplimientoEntrenamiento.evaluar(session).diaCumplido()).isFalse();
        var second = Serie.builder().id(2L).diaEjercicio(plan).numeroSerie(2).repsHechas(8).completada(true).build();
        session.getSeries().add(second);
        assertThat(CumplimientoEntrenamiento.evaluar(session).diaCumplido()).isTrue();
        second.setNumeroSerie(3);
        assertThat(CumplimientoEntrenamiento.evaluar(session).diaCumplido()).isFalse();
        second.setNumeroSerie(2); second.setAnulada(true);
        assertThat(CumplimientoEntrenamiento.evaluar(session).seriesRealizadas()).isEqualTo(1);
        assertThat(CumplimientoEntrenamiento.evaluar(session).diaCumplido()).isFalse();
        first.setLegadoExento(true);
        assertThat(CumplimientoEntrenamiento.evaluar(session).seriesRealizadas()).isZero();
    }
}
