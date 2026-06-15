package utn.simulacro_nombreAlumno.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utn.simulacro_nombreAlumno.model.request.LoginRequest;
import utn.simulacro_nombreAlumno.model.request.RegisterAlumnoRequest;
import utn.simulacro_nombreAlumno.model.request.RegisterProfesorRequest;
import utn.simulacro_nombreAlumno.model.response.AuthResponse;
import utn.simulacro_nombreAlumno.service.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/alumno")
    public ResponseEntity<AuthResponse> registerAlumno(@Valid @RequestBody RegisterAlumnoRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerAlumno(request));
    }

    @PostMapping("/register/profesor")
    public ResponseEntity<AuthResponse> registerProfesor(@Valid @RequestBody RegisterProfesorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerProfesor(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
