package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.request.AlumnoRequest;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.service.AlumnoService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;


    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarTodos() {
        return ResponseEntity.ok(alumnoService.listarAlumnos());
    }

    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.findAlumnoResponseById(id));
    }

    @PreAuthorize("hasRole('ALUMNO')")
    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> update(@PathVariable Long id, @Valid @RequestBody AlumnoRequest request) {
        return ResponseEntity.ok(alumnoService.updateAlumno(id, request));
    }
    @PreAuthorize("hasRole('ALUMNO')")
    @PostMapping("/{alumnoId}/profesores/{profesorId}")
    public ResponseEntity<AlumnoResponse> elegirProfesor(@PathVariable Long alumnoId, @PathVariable Long profesorId) {
        return ResponseEntity.ok(alumnoService.elegirProfesor(alumnoId, profesorId));
    }

}

