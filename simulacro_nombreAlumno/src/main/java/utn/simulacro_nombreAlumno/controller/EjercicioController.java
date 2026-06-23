package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.GrupoMuscular;
import utn.simulacro_nombreAlumno.model.request.EjercicioRequest;
import utn.simulacro_nombreAlumno.model.response.EjercicioResponse;
import utn.simulacro_nombreAlumno.service.EjercicioService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ejercicios")
public class EjercicioController {

    private final EjercicioService ejercicioService;

    @GetMapping
    public ResponseEntity<List<EjercicioResponse>> findAll(
            @RequestParam(required = false) GrupoMuscular grupoMuscular) {
        if (grupoMuscular != null) {
            return ResponseEntity.ok(ejercicioService.listarPorGrupoMuscular(grupoMuscular));
        }
        return ResponseEntity.ok(ejercicioService.listarEjercicios());
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EjercicioResponse> create(@Valid @RequestBody EjercicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ejercicioService.createEjercicio(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<EjercicioResponse> update(@PathVariable Long id, @Valid @RequestBody EjercicioRequest request) {
        return ResponseEntity.ok(ejercicioService.updateEjercicio(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ejercicioService.deleteEjercicio(id);
        return ResponseEntity.noContent().build();
    }
}

