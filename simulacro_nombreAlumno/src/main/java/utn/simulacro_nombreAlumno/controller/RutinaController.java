package utn.simulacro_nombreAlumno.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<List<RutinaResponse>> listarTodas (){
         return ResponseEntity.ok(rutinaService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<RutinaResponse> crearRutina(@Valid @RequestBody RutinaRequest request) {
        RutinaResponse response = rutinaService.crearRutina(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RutinaResponse> update(@PathVariable Long id, @Valid @RequestBody RutinaRequest request) {
        return ResponseEntity.ok(rutinaService.updateRutina(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        rutinaService.deleteRutina(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/asignar")
    public ResponseEntity<AsignacionResponse> asignar(@Valid @RequestBody AsignacionRutinaRequest asignacionRutinaRequest) {
        return ResponseEntity.ok(rutinaService.asignarRutina(asignacionRutinaRequest));
    }

    @GetMapping("/activa/alumno/{alumnoId}")
    public ResponseEntity<RutinaResponse> getRutinaActiva(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(rutinaService.getRutinaActivaDeAlumno(alumnoId));
    }

    @GetMapping("/historial/alumno/{alumnoId}")
    public ResponseEntity<List<RutinaResponse>> getHistorial(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(rutinaService.getHistorialDeAlumno(alumnoId));
    }
}
