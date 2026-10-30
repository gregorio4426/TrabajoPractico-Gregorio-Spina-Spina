package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.request.ProfesorRequest;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.service.ProfesorService;

import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.service.AlumnoService;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profesores")
public class ProfesorController {

    private final ProfesorService profesorService;
    private final AlumnoService alumnoService;

    @PreAuthorize("hasAnyRole('ADMIN','ALUMNO','PROFESOR')")
    @GetMapping
    public ResponseEntity<List<ProfesorResponse>> listarTodos() {
        return ResponseEntity.ok(profesorService.listarProfesores());
    }

    @PreAuthorize("hasAnyRole('ADMIN','ALUMNO','PROFESOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ProfesorResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(profesorService.findProfesorResponseById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")
    @PatchMapping("/{id}")
    public ResponseEntity<ProfesorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProfesorRequest request) {
        return ResponseEntity.ok(profesorService.updateProfesor(id, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<ProfesorResponse> getMiPerfil(Authentication authentication) {
        return ResponseEntity.ok(profesorService.getMiPerfil(authentication));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<ProfesorResponse> updateMiPerfil(
            @Valid @RequestBody ProfesorRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(profesorService.updateMiPerfil(request, authentication));
    }

    @GetMapping("/me/alumnos")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<List<AlumnoResponse>> misAlumnos(Authentication auth) {
        return ResponseEntity.ok(alumnoService.misAlumnos(auth));
    }

    @DeleteMapping("/me/alumnos/{alumnoId}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<Void> desvincularAlumno(@PathVariable Long alumnoId, Authentication auth) {
        alumnoService.desvincularMiAlumno(alumnoId, auth);
        return ResponseEntity.noContent().build();
    }
}
