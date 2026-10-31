package utn.simulacro_nombreAlumno.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import utn.simulacro_nombreAlumno.model.response.ErrorResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component @RequiredArgsConstructor
public class SecurityErrorWriter {
    private final ObjectMapper json;
    public void unauthorized(HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, 401, "Autenticación requerida o credenciales inválidas");
    }
    public void forbidden(HttpServletResponse response) throws IOException { write(response,403,"No tenés permiso para esta operación"); }
    private void write(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), new ErrorResponse(message, LocalDateTime.now()));
    }
}
