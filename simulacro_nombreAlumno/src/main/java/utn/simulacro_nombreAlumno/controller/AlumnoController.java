package utn.simulacro_nombreAlumno.controller;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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


    @PostMapping
    public ResponseEntity<AlumnoResponse> registrar(@Valid @RequestBody AlumnoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alumnoService.createAlumno(request));
    }

    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarTodos (){
        return ResponseEntity.ok(alumnoService.listarAlumnos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.findAlumnoResponseById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlumnoResponse> update(@PathVariable Long id, @Valid @RequestBody AlumnoRequest request) {
        return ResponseEntity.ok(alumnoService.updateAlumno(id, request));
    }


}

