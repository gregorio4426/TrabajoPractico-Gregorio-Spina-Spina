package utn.simulacro_nombreAlumno.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.request.SerieRequest;
import utn.simulacro_nombreAlumno.model.request.SesionRequest;
import utn.simulacro_nombreAlumno.model.response.SerieResponse;
import utn.simulacro_nombreAlumno.model.response.SesionResponse;
import utn.simulacro_nombreAlumno.service.SesionService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sesiones")
public class SesionController {

    private final SesionService sesionService;

    @PostMapping
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SesionResponse> iniciarSesion(
            @Valid @RequestBody SesionRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sesionService.iniciarSesion(request, authentication));
    }

    @PostMapping("/{id}/series")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SerieResponse> registrarSerie(
            @PathVariable Long id,
            @Valid @RequestBody SerieRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sesionService.registrarSerie(id, request, authentication));
    }

    @PutMapping("/{id}/completar")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SesionResponse> completarSesion(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(sesionService.completarSesion(id, authentication));
    }

    @GetMapping("/me/historial")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<List<SesionResponse>> getHistorialMe(Authentication authentication) {
        return ResponseEntity.ok(sesionService.getHistorialMe(authentication));
    }

    @GetMapping("/alumno/{alumnoId}/historial")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<List<SesionResponse>> getHistorialDeAlumno(
            @PathVariable Long alumnoId,
            Authentication authentication) {
        return ResponseEntity.ok(sesionService.getHistorialDeAlumno(alumnoId, authentication));
    }
}