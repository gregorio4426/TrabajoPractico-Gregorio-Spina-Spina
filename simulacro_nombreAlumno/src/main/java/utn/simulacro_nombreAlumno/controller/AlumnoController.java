package utn.simulacro_nombreAlumno.controller;
import ch.qos.logback.core.read.ListAppender;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.service.AlumnoService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;


    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> listarTodos (){
        return ResponseEntity.ok(alumnoService.listarAlumnos());
    }


}

