package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.request.EjercicioRequest;
import utn.simulacro_nombreAlumno.model.response.EjercicioResponse;
import utn.simulacro_nombreAlumno.service.EjercicioService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ejercicios")
public class EjercicioController {

    private final EjercicioService ejercicioService;

    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping
    public ResponseEntity<List<EjercicioResponse>> findAll() {
        return ResponseEntity.ok(ejercicioService.listarEjercicios());
    }


    @PreAuthorize("hasAnyRole('ALUMNO', 'PROFESOR')")
    @GetMapping("/{id}")
    public ResponseEntity<EjercicioResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ejercicioService.findEjercicioResponseById(id));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PostMapping
    public ResponseEntity<EjercicioResponse> create(@Valid @RequestBody EjercicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ejercicioService.createEjercicio(request));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @PutMapping("/{id}")
    public ResponseEntity<EjercicioResponse> update(@PathVariable Long id, @Valid @RequestBody EjercicioRequest request) {
        return ResponseEntity.ok(ejercicioService.updateEjercicio(id, request));
    }

    @PreAuthorize("hasRole('PROFESOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ejercicioService.deleteEjercicio(id);
        return ResponseEntity.noContent().build();
    }

}

