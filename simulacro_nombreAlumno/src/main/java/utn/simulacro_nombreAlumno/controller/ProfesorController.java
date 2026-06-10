package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.request.ProfesorRequest;
import utn.simulacro_nombreAlumno.model.response.ProfesorResponse;
import utn.simulacro_nombreAlumno.service.ProfesorService;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profesores")
public class ProfesorController {

    private final ProfesorService profesorService ;

    @PutMapping("{id}")
    public ResponseEntity<ProfesorResponse> updateProfesor(@PathVariable Long id, @Valid @RequestBody ProfesorRequest cambios) {

        return ResponseEntity.ok(profesorService.updateProfesor(id, cambios));
    }

    @PostMapping
    public ResponseEntity<ProfesorResponse> registrar(@Valid @RequestBody ProfesorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesorService.createProfesor(request));
    }

    @GetMapping
    public ResponseEntity<List<ProfesorResponse>> listarTodos() {
        return ResponseEntity.ok(profesorService.listarProfesores());
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProfesorResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(profesorService.findProfesorResponseById(id));
    }
}

