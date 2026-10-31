package utn.simulacro_nombreAlumno.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import utn.simulacro_nombreAlumno.model.response.*;
import java.util.List;
import static utn.simulacro_nombreAlumno.admin.AdminDtos.*;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminConsultaService consultas;
    private final AdminGestionService gestion;
    @GetMapping("/usuarios") public Pagina<UsuarioVista> usuarios(@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.usuarios(f,a);}
    @GetMapping("/usuarios/{id}") public UsuarioVista usuario(@PathVariable Long id,Authentication a){return consultas.usuario(id,a);}
    @PostMapping("/usuarios") @ResponseStatus(HttpStatus.CREATED) public UsuarioVista crear(@Valid @RequestBody CrearUsuario r,Authentication a){return gestion.crear(r,a);}
    @PutMapping("/usuarios/{id}/estado") public UsuarioVista estado(@PathVariable Long id,@Valid @RequestBody CambioEstado r,Authentication a){return gestion.estado(id,r,a);}
    @PutMapping("/usuarios/{id}/rol") public UsuarioVista rol(@PathVariable Long id,@Valid @RequestBody CambioRol r,Authentication a){return gestion.rol(id,r,a);}
    @PutMapping("/usuarios/{id}/email") public UsuarioVista email(@PathVariable Long id,@Valid @RequestBody CambioEmail r,Authentication a){return gestion.email(id,r,a);}
    @GetMapping("/alumnos") public Pagina<AlumnoVista> alumnos(@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.alumnos(f,a);}
    @GetMapping("/alumnos/{id}") public AlumnoVista alumno(@PathVariable Long id,Authentication a){return consultas.alumno(id,a);}
    @PostMapping("/alumnos") @ResponseStatus(HttpStatus.CREATED) public AlumnoVista crearAlumno(@Valid @RequestBody CrearAlumno r,Authentication a){return gestion.crearAlumno(r,a);}
    @PostMapping("/alumnos/{id}/cuenta") @ResponseStatus(HttpStatus.CREATED) public UsuarioVista cuentaAlumno(@PathVariable Long id,@Valid @RequestBody CrearCuentaPerfil r,Authentication a){return gestion.crearCuentaPerfil(id,true,r,a);}
    @PutMapping("/alumnos/{id}") public AlumnoVista alumno(@PathVariable Long id,@Valid @RequestBody AlumnoDatos r,Authentication a){return gestion.alumno(id,r,a);}
    @GetMapping("/profesores") public Pagina<ProfesorVista> profesores(@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.profesores(f,a);}
    @GetMapping("/profesores/{id}") public ProfesorVista profesor(@PathVariable Long id,Authentication a){return consultas.profesor(id,a);}
    @PostMapping("/profesores") @ResponseStatus(HttpStatus.CREATED) public ProfesorVista crearProfesor(@Valid @RequestBody CrearProfesor r,Authentication a){return gestion.crearProfesor(r,a);}
    @PostMapping("/profesores/{id}/cuenta") @ResponseStatus(HttpStatus.CREATED) public UsuarioVista cuentaProfesor(@PathVariable Long id,@Valid @RequestBody CrearCuentaPerfil r,Authentication a){return gestion.crearCuentaPerfil(id,false,r,a);}
    @PutMapping("/profesores/{id}") public ProfesorVista profesor(@PathVariable Long id,@Valid @RequestBody ProfesorDatos r,Authentication a){return gestion.profesor(id,r,a);}
    @PutMapping("/alumnos/{alumnoId}/profesores/{profesorId}") public AlumnoVista vincular(@PathVariable Long alumnoId,@PathVariable Long profesorId,@Valid @RequestBody CambioVinculo r,Authentication a){return gestion.vinculo(alumnoId,profesorId,r,true,a);}
    @DeleteMapping("/alumnos/{alumnoId}/profesores/{profesorId}") public AlumnoVista desvincular(@PathVariable Long alumnoId,@PathVariable Long profesorId,@Valid @RequestBody CambioVinculo r,Authentication a){return gestion.vinculo(alumnoId,profesorId,r,false,a);}
    @PutMapping("/alumnos/{alumnoId}/profesores/reemplazo") public AlumnoVista reemplazar(@PathVariable Long alumnoId,@Valid @RequestBody ReemplazoVinculo r,Authentication a){return gestion.reemplazar(alumnoId,r,a);}
    @GetMapping("/profesores/{id}/alumnos") public Pagina<AlumnoVista> alumnosProfesor(@PathVariable Long id,@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){consultas.profesor(id,a);f.setProfesorId(id);return consultas.alumnos(f,a);}
    @GetMapping("/alumnos/{id}/profesores") public Pagina<ProfesorVista> profesoresAlumno(@PathVariable Long id,@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){consultas.alumno(id,a);f.setAlumnoId(id);return consultas.profesores(f,a);}
    @GetMapping("/rutinas") public Pagina<RutinaResponse> rutinas(@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.rutinas(f,a);}
    @GetMapping("/rutinas/{id}") public RutinaResponse rutina(@PathVariable Long id,Authentication a){return consultas.rutina(id,a);}
    @GetMapping("/rutinas/{id}/versiones") public Pagina<RutinaResponse> versiones(@PathVariable Long id,@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.versiones(id,f,a);}
    @GetMapping("/asignaciones") public Pagina<AsignacionResponse> asignaciones(@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.asignaciones(f,a);}
    @GetMapping("/asignaciones/{id}") public AsignacionResponse asignacion(@PathVariable Long id,Authentication a){return consultas.asignacion(id,a);}
    @GetMapping("/alumnos/{id}/historial") public Pagina<AsignacionResponse> historial(@PathVariable Long id,@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){consultas.alumno(id,a);f.setAlumnoId(id);return consultas.asignaciones(f,a);}
    @GetMapping("/sesiones") public Pagina<SesionResponse> sesiones(@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.sesiones(f,a);}
    @GetMapping("/alumnos/{id}/sesiones") public Pagina<SesionResponse> sesionesAlumno(@PathVariable Long id,@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){consultas.alumno(id,a);f.setAlumnoId(id);return consultas.sesiones(f,a);}
    @GetMapping("/sesiones/{id}") public SesionResponse sesion(@PathVariable Long id,Authentication a){return consultas.sesion(id,a);}
    @GetMapping("/sesiones/{id}/auditoria") public List<AuditoriaEntrenamientoResponse> auditoriaSesion(@PathVariable Long id,Authentication a){return consultas.auditoriaSesion(id,a);}
    @GetMapping("/resumen") public Resumen resumen(Authentication a){return consultas.resumen(a);}
    @GetMapping("/auditoria") public Pagina<AuditoriaVista> auditoria(@RequestParam(required=false) Long usuarioId,@Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute AdminFiltro f,Authentication a){return consultas.auditoria(usuarioId,f,a);}
}
