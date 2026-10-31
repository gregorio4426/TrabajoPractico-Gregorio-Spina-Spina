package utn.simulacro_nombreAlumno.security;

import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import java.nio.charset.StandardCharsets;
public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static void validate(String password, int minimum) {
        if (password == null || password.isBlank() || password.length() < minimum || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ReglaNegocioException("La contraseña debe cumplir la longitud mínima y no superar 72 bytes UTF-8");
        }
    }
}
