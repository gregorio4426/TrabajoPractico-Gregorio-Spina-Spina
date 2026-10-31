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
                if (!ordenes.add(dia.getNumeroOrden())) throw new ReglaNegocioException("Los números de orden de los días no pueden repetirse");
            }
        }
    }
}
