package utn.simulacro_nombreAlumno.controller;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import utn.simulacro_nombreAlumno.service.RutinaService;
import utn.simulacro_nombreAlumno.model.request.PesosAsignacionRequest;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
@RestController @RequiredArgsConstructor @RequestMapping("/api/asignaciones")
public class AsignacionPesosController {
 private final RutinaService service;
 @GetMapping("/{id}") public RutinaResponse get(@PathVariable Long id,Authentication auth){return service.getAsignacion(id,auth);}
 @PutMapping("/{id}/pesos") @PreAuthorize("hasRole('PROFESOR')")
 public RutinaResponse editar(@PathVariable Long id,@Valid @RequestBody PesosAsignacionRequest datos,Authentication auth){return service.editarPesos(id,datos,auth);}
}
