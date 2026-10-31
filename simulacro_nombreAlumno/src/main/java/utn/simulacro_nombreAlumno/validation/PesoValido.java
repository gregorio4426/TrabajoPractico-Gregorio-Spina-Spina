package utn.simulacro_nombreAlumno.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PesoValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PesoValido {
    String message() default "debe ser finito y no negativo; null significa no registrado";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
