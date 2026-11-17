package utn.simulacro_nombreAlumno.controller;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import utn.simulacro_nombreAlumno.service.GrupoMuscularService;
import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/grupos-musculares")
public class GrupoMuscularController {
 private final GrupoMuscularService service;
 public record Datos(@NotBlank @Size(max=100) String nombre) {}
 @GetMapping public List<GrupoMuscularService.Vista> listar(){return service.listar();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('PROFESOR')")
 public GrupoMuscularService.Vista crear(@Valid @RequestBody Datos datos){return service.crear(datos.nombre());}
 @PutMapping("/{id}") @PreAuthorize("hasRole('PROFESOR')")
 public GrupoMuscularService.Vista editar(@PathVariable Long id,@Valid @RequestBody Datos datos){return service.editar(id,datos.nombre());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('PROFESOR')")
 public void eliminar(@PathVariable Long id){service.eliminar(id);}
}
