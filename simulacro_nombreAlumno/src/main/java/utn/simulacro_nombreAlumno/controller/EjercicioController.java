package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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


    @GetMapping
    public ResponseEntity<List<EjercicioResponse>> findAll() {
        return ResponseEntity.ok(ejercicioService.listarEjercicios());
    }


    @GetMapping("/{id}")
    public ResponseEntity<EjercicioResponse> findById(@PathVariable  Long id) {
        return ResponseEntity.ok(ejercicioService.findEjercicioResponseById(id));
    }

    @PostMapping
    public ResponseEntity<EjercicioResponse> create(@Valid @RequestBody EjercicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ejercicioService.createEjercicio(request));
    }



}

