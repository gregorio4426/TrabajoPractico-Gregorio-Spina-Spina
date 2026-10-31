package utn.simulacro_nombreAlumno.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import utn.simulacro_nombreAlumno.model.response.ErrorResponse;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Getter
@Setter
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(errores, LocalDateTime.now()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("No tenés permiso para esta operación", LocalDateTime.now()));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(jakarta.validation.ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream().map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted().collect(java.util.stream.Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(mensaje, LocalDateTime.now()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Solicitud JSON inválida o valores incompatibles", LocalDateTime.now()));
    }

    @ExceptionHandler({ConflictoEntrenamientoException.class, org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.OptimisticLockingFailureException.class, org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Conflicto de estado o integridad; recargá el registro antes de reintentar", LocalDateTime.now()));
    }

    @ExceptionHandler(org.springframework.orm.jpa.JpaSystemException.class)
    public ResponseEntity<ErrorResponse> handleJpa(org.springframework.orm.jpa.JpaSystemException ex) {
        // MySQL CHECK (3819/HY000) no siempre se traduce a DataIntegrityViolationException.
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && (sql.getErrorCode() == 3819
                    || (sql.getSQLState() != null && sql.getSQLState().startsWith("23")))) {
                return handleConflict(ex);
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("No se pudo persistir la operación", LocalDateTime.now()));
    }

    @ExceptionHandler({org.springframework.security.core.AuthenticationException.class, io.jsonwebtoken.JwtException.class})
    public ResponseEntity<ErrorResponse> handleAuthentication(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("WWW-Authenticate", "Bearer")
                .body(new ErrorResponse("Autenticación requerida o credenciales inválidas", LocalDateTime.now()));
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.ServletRequestBindingException.class,
            org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Parámetros de solicitud inválidos", LocalDateTime.now()));
    }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class, org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleMissingRoute(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Recurso no encontrado", LocalDateTime.now()));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(Exception ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ErrorResponse("Método no permitido", LocalDateTime.now()));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMedia(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ErrorResponse("Formato de solicitud no soportado", LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error interno del servidor", LocalDateTime.now()));
    }
}