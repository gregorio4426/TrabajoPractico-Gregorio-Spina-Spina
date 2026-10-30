package utn.simulacro_nombreAlumno.model.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record AuditoriaEntrenamientoResponse(Long id, Long sesionId, Long serieId, Long actorId,
        String accion, String motivo, JsonNode antes, JsonNode despues, LocalDateTime creadaEn) {}
