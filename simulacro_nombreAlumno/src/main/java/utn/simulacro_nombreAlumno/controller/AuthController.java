package utn.simulacro_nombreAlumno.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final utn.simulacro_nombreAlumno.service.IdentidadService identidad;

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @io.swagger.v3.oas.annotations.Operation(summary="Registro público exclusivamente de ALUMNO")
    @PostMapping("/register/alumno")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> registerAlumno(@Valid @RequestBody RegisterAlumnoRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerAlumno(request));
    }


    @PostMapping("/register/profesor")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> registerProfesor(@Valid @RequestBody RegisterProfesorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerProfesor(request));
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    @io.swagger.v3.oas.annotations.Operation(summary="Login único para ADMIN, PROFESOR y ALUMNO",description="Devuelve JWT, email, rol y perfilId del rol vigente. Enviar el token como Authorization: Bearer en las rutas protegidas.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }



    @org.springframework.web.bind.annotation.PutMapping("/me/email")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR','ALUMNO')")
    public ResponseEntity<AuthResponse> cambiarEmail(@Valid @RequestBody utn.simulacro_nombreAlumno.model.request.CambioEmailRequest request,
                                                    org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(identidad.cambiarEmail(request, auth));
    }

    @org.springframework.web.bind.annotation.PutMapping("/me/password")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESOR','ALUMNO')")
    public ResponseEntity<AuthResponse> cambiarPassword(@Valid @RequestBody utn.simulacro_nombreAlumno.model.request.CambioPasswordRequest request,
                                                       org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(identidad.cambiarPassword(request, auth));
    }
}
