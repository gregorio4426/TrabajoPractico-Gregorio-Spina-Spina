package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping("/{id}")
    public ResponseEntity<RutinaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(rutinaService.findById(id));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping
    public ResponseEntity<RutinaResponse> crear(@Valid @RequestBody RutinaRequest request , Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rutinaService.crearRutina(request ,authentication ));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PutMapping("/{id}")
    public ResponseEntity<RutinaResponse> update(@PathVariable Long id, @Valid @RequestBody RutinaRequest request , Authentication authentication) {
        return ResponseEntity.ok(rutinaService.updateRutina(id, request ,authentication));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        rutinaService.deleteRutina(id, authentication);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/activa/alumno/{alumnoId}")
    public ResponseEntity<RutinaResponse> getRutinaActiva(@PathVariable Long alumnoId, Authentication authentication) {
        return ResponseEntity.ok(rutinaService.getRutinaActivaDeAlumno(alumnoId, authentication));
    }

    @GetMapping("/historial/alumno/{alumnoId}")
    public ResponseEntity<List<RutinaResponse>> getHistorial(@PathVariable Long alumnoId, Authentication authentication) {
        return ResponseEntity.ok(rutinaService.getHistorialDeAlumno(alumnoId, authentication));
    }


    @PostMapping("/asignar/alumno/{alumnoId}/rutina/{rutinaId}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<AsignacionResponse> asignar(@PathVariable Long alumnoId, @PathVariable Long rutinaId, Authentication authentication) {
        return ResponseEntity.ok(rutinaService.asignarRutinaMe(alumnoId, rutinaId, authentication));
    }

    @GetMapping("/me/activa")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<RutinaResponse> getRutinaActivaMe(Authentication authentication) {
        return ResponseEntity.ok(rutinaService.getRutinaActivaMe(authentication));
    }

    @GetMapping("/me/historial")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<List<RutinaResponse>> getHistorialMe(Authentication authentication) {
        return ResponseEntity.ok(rutinaService.getHistorialMe(authentication));
    }
}
