package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.request.AlumnoRequest;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;
import utn.simulacro_nombreAlumno.service.AlumnoService;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;



    @PreAuthorize("hasAnyRole('PROFESOR')")
    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarTodos() {
        return ResponseEntity.ok(alumnoService.listarAlumnos());
    }
    @PreAuthorize("hasAnyRole( 'PROFESOR')")
    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.findAlumnoResponseById(id));
    }

    @PreAuthorize("hasRole('ALUMNO')")
    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> update(
            @PathVariable Long id, @Valid @RequestBody AlumnoRequest request, Authentication authentication) {
        return ResponseEntity.ok(alumnoService.updateAlumno(id, request, authentication));
    }
    @PreAuthorize("hasRole('ALUMNO')")
    @PostMapping("/{alumnoId}/profesores/{profesorId}")
    public ResponseEntity<AlumnoResponse> elegirProfesor(@PathVariable Long alumnoId, @PathVariable Long profesorId, Authentication authentication) {
        return ResponseEntity.ok(alumnoService.elegirProfesor(alumnoId, profesorId, authentication));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<AlumnoResponse> getMiPerfil(Authentication authentication) {
        return ResponseEntity.ok(alumnoService.getMiPerfil(authentication));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<AlumnoResponse> updateMiPerfil(
            @Valid @RequestBody AlumnoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(alumnoService.updateMiPerfil(request, authentication));
    }

    @PostMapping("/me/profesores/{profesorId}")
    @PreAuthorize("hasRole('ALUMNO')")
    public ResponseEntity<AlumnoResponse> elegirProfesor(
            @PathVariable Long profesorId,
            Authentication authentication) {
        return ResponseEntity.ok(alumnoService.elegirProfesorMe(profesorId, authentication));
    }

}

