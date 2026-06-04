package utn.simulacro_nombreAlumno.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
}

