package utn.simulacro_nombreAlumno.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PesoValidoValidator implements ConstraintValidator<PesoValido, Double> {
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        return value == null || (Double.isFinite(value) && value >= 0);
    }
}
