package utn.simulacro_nombreAlumno.validation;

import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import utn.simulacro_nombreAlumno.model.request.RutinaRequest;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class ValidacionEntrenamiento {
    private final Validator validator;
    public void validar(Object request, Class<?>... groups) {
        if (request == null) throw new ReglaNegocioException("Solicitud requerida");
        var violations = validator.validate(request, groups);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }
    public void rutina(RutinaRequest request, boolean creacion) {
        validar(request, creacion ? CreacionRutina.class : jakarta.validation.groups.Default.class);
        if (request.getDias() != null) {
            var ordenes = new HashSet<Integer>();
            for (var dia : request.getDias()) {
                if(dia.getNombre()==null || dia.getNombre().isBlank()) dia.setNombre("Día " + dia.getNumeroOrden());
                var bloques = new java.util.HashMap<Integer, java.util.List<Object>>();
                var posiciones = new java.util.HashSet<String>();
                for(var ejercicio:dia.getEjercicios()) {
                    var tipo=ejercicio.getTipoMedicion()==null?utn.simulacro_nombreAlumno.model.TipoMedicion.REPETICIONES:ejercicio.getTipoMedicion();
                    if(tipo==utn.simulacro_nombreAlumno.model.TipoMedicion.SEGUNDOS ? ejercicio.getRepsSugeridas()!=null : ejercicio.getSegundosSugeridos()!=null) throw new ReglaNegocioException("No mezcles segundos y repeticiones en la misma prescripción");
                    if(ejercicio.getPesoSugerido()!=null)throw new ReglaNegocioException("El peso sugerido se configura al asignar la rutina a un alumno, no en la plantilla");
                    if(ejercicio.getBloqueOrden()==null) {
                        if(ejercicio.getVueltas()!=null || ejercicio.getOrdenEnBloque()!=null || ejercicio.getBloqueNombre()!=null || ejercicio.getDescansoEjercicioSegundos()!=null || ejercicio.getDescansoVueltaSegundos()!=null)
                            throw new ReglaNegocioException("La configuración de bloque requiere un orden de bloque");
                        continue;
                    }
                    if(ejercicio.getVueltas()==null || ejercicio.getOrdenEnBloque()==null || !ejercicio.getVueltas().equals(ejercicio.getSeriesSugeridas())) throw new ReglaNegocioException("Cada ejercicio del bloque debe respetar sus vueltas y orden");
                    if(!posiciones.add(ejercicio.getBloqueOrden()+":"+ejercicio.getOrdenEnBloque()))throw new ReglaNegocioException("Orden de ejercicio repetido en el bloque");
                    var config=java.util.Arrays.<Object>asList(ejercicio.getBloqueNombre(),ejercicio.getVueltas(),ejercicio.getDescansoVueltaSegundos());
                    var anterior=bloques.putIfAbsent(ejercicio.getBloqueOrden(),config);
                    if(anterior!=null && !anterior.equals(config))throw new ReglaNegocioException("Los ejercicios deben compartir la configuración de su bloque");
                }
                if (!ordenes.add(dia.getNumeroOrden())) throw new ReglaNegocioException("Los números de orden de los días no pueden repetirse");
            }
        }
    }
}
