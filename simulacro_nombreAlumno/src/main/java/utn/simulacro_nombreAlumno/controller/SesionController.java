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
import utn.simulacro_nombreAlumno.model.response.UltimoEjercicioResponse;
import utn.simulacro_nombreAlumno.service.SesionService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sesiones")
public class SesionController {
    private final SesionService sesionService;

    @PostMapping
    @PreAuthorize("hasRole('ALUMNO')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SesionResponse> iniciarSesion(@Valid @RequestBody SesionRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sesionService.iniciarSesion(request, authentication));
    }

    @PostMapping("/{id}/series")
    @PreAuthorize("hasRole('ALUMNO')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SerieResponse> registrarSerie(@PathVariable Long id, @Valid @RequestBody SerieRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sesionService.registrarSerie(id, request, authentication));
    }

    @PutMapping("/{id}/completar")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SesionResponse> completarSesion(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.completarSesion(id, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ALUMNO','PROFESOR')")
    public ResponseEntity<SesionResponse> getDetalle(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.getDetalle(id, authentication));
    }

    @GetMapping("/me/historial")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<List<SesionResponse>> getHistorialMe(Authentication authentication) {
        return ResponseEntity.ok(sesionService.getHistorialMe(authentication));
    }

    @GetMapping("/me/ejercicios/{ejercicioId}/ultima")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<UltimoEjercicioResponse> getUltimoEjercicio(@PathVariable Long ejercicioId, Authentication authentication) {
        return ResponseEntity.ok(sesionService.getUltimoEjercicio(ejercicioId, authentication));
    }

    @GetMapping("/alumno/{alumnoId}/historial")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")
    public ResponseEntity<List<SesionResponse>> getHistorialDeAlumno(@PathVariable Long alumnoId, Authentication authentication) {
        return ResponseEntity.ok(sesionService.getHistorialDeAlumno(alumnoId, authentication));
    }

    @PatchMapping("/{id}/series/{serieId}")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SerieResponse> corregirSerie(@PathVariable Long id, @PathVariable Long serieId,
            @Valid @RequestBody utn.simulacro_nombreAlumno.model.request.CorreccionSerieRequest request, Authentication auth) {
        return ResponseEntity.ok(sesionService.corregirSerie(id, serieId, request, auth));
    }

    @DeleteMapping("/{id}/series/{serieId}")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SerieResponse> anularSerie(@PathVariable Long id, @PathVariable Long serieId,
            @Valid @RequestBody utn.simulacro_nombreAlumno.model.request.AnulacionSerieRequest request, Authentication auth) {
        return ResponseEntity.ok(sesionService.anularSerie(id, serieId, request, auth));
    }

    @PatchMapping("/{id}/correccion")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SesionResponse> corregirSesion(@PathVariable Long id,
            @Valid @RequestBody utn.simulacro_nombreAlumno.model.request.CorreccionSesionRequest request, Authentication auth) {
        return ResponseEntity.ok(sesionService.corregirSesion(id, request, auth));
    }

    @GetMapping("/{id}/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR','ALUMNO')")
    public ResponseEntity<List<utn.simulacro_nombreAlumno.model.response.AuditoriaEntrenamientoResponse>> auditoria(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(sesionService.auditoria(id, auth));
    }

    @PutMapping("/{id}/series/{serieId}/restaurar")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<SerieResponse> restaurarSerie(@PathVariable Long id, @PathVariable Long serieId,
            @Valid @RequestBody utn.simulacro_nombreAlumno.model.request.CorreccionSerieRequest request, Authentication auth) {
        return ResponseEntity.ok(sesionService.restaurarSerie(id, serieId, request, auth));
    }
}
