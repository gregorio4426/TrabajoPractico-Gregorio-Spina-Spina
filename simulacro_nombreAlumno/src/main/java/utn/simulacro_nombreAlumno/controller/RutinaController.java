package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.request.AsignacionRutinaRequest;
import utn.simulacro_nombreAlumno.model.request.RutinaRequest;
import utn.simulacro_nombreAlumno.model.response.AsignacionResponse;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
import utn.simulacro_nombreAlumno.service.RutinaService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rutinas")
public class RutinaController {

    private final RutinaService rutinaService;

    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping
    public ResponseEntity<List<RutinaResponse>> listarTodas (){
         return ResponseEntity.ok(rutinaService.listarTodas());
    }

    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping("/{id}")
    public ResponseEntity<RutinaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rutinaService.findById(id));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping
    public ResponseEntity<RutinaResponse> crear(@Valid @RequestBody RutinaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rutinaService.crearRutina(request));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PutMapping("/{id}")
    public ResponseEntity<RutinaResponse> update(@PathVariable Long id, @Valid @RequestBody RutinaRequest request) {
        return ResponseEntity.ok(rutinaService.updateRutina(id, request));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rutinaService.deleteRutina(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping("/{rutinaId}/asignar/{alumnoId}")
    public ResponseEntity<AsignacionResponse> asignar(@PathVariable Long rutinaId, @PathVariable Long alumnoId) {
        return ResponseEntity.ok(rutinaService.asignarRutina(rutinaId, alumnoId));
    }

    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping("/activa/alumno/{alumnoId}")
    public ResponseEntity<RutinaResponse> getRutinaActiva(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(rutinaService.getRutinaActivaDeAlumno(alumnoId));
    }

    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping("/historial/alumno/{alumnoId}")
    public ResponseEntity<List<RutinaResponse>> getHistorial(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(rutinaService.getHistorialDeAlumno(alumnoId));
    }
}
