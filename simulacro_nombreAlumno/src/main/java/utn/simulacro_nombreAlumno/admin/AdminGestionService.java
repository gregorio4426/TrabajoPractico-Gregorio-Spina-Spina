package utn.simulacro_nombreAlumno.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.*;
import utn.simulacro_nombreAlumno.service.*;
import utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento;
import java.time.*;
import java.util.*;
import static utn.simulacro_nombreAlumno.admin.AdminDtos.*;

@Service @RequiredArgsConstructor @Transactional @PreAuthorize("hasRole('ADMIN')")
public class AdminGestionService {
    private final AdminGuard guard;
    private final AdminConsultaService consulta;
    private final IdentidadService identidad;
    private final UsuarioEstadoService estados;
    private final UsuarioRepository usuarios;
    private final AlumnoRepository alumnos;
    private final ProfesorRepository profesores;
    private final AuditoriaAdministrativaRepository auditorias;
    private final AlumnoService alumnoService;
    private final PasswordEncoder encoder;
    private final ValidacionEntrenamiento validacion;
    private final ObjectMapper json;

    public UsuarioVista crear(CrearUsuario r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);
        validarPerfilRol(r.rol(),r.alumno(),r.profesor(),false);
        PasswordPolicy.validate(r.password(),r.rol()==Rol.ADMIN?12:8);
        String email=EmailNormalizer.normalize(r.email());identidad.exigirEmailDisponible(email,null);
        Usuario u=usuarios.saveAndFlush(Usuario.builder().email(email).password(encoder.encode(r.password())).rol(r.rol()).build());
        if(r.alumno()!=null) crearAlumno(u,r.alumno());
        if(r.profesor()!=null) crearProfesor(u,r.profesor());
        usuarios.flush();
        auditar(actor,u,"CREAR_CUENTA","Alta administrativa",Map.of(),AdminConsultaService.usuarioVista(u));
        return AdminConsultaService.usuarioVista(u);
    }
    public AlumnoVista crearAlumno(CrearAlumno r,Authentication auth) {
        guard.bloquear(auth);validacion.validar(r);
        UsuarioVista u=crear(new CrearUsuario(r.email(),r.password(),Rol.ALUMNO,r.perfil(),null),auth);
        return consulta.alumno(u.alumnoId(),auth);
    }
    public ProfesorVista crearProfesor(CrearProfesor r,Authentication auth) {
        guard.bloquear(auth);validacion.validar(r);
        UsuarioVista u=crear(new CrearUsuario(r.email(),r.password(),Rol.PROFESOR,null,r.perfil()),auth);
        return consulta.profesor(u.profesorId(),auth);
    }
    /** Regulariza únicamente un perfil legado sin cuenta; no fusiona identidades ni cambia sus IDs. */
    public UsuarioVista crearCuentaPerfil(Long perfilId,boolean esAlumno,CrearCuentaPerfil r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);PasswordPolicy.validate(r.password(),8);
        Alumno a=esAlumno?alumnos.findByIdForUpdate(perfilId).orElseThrow(()->new RecursoNoEncontradoException("Alumno no encontrado")):null;
        Profesor p=esAlumno?null:profesores.findById(perfilId).orElseThrow(()->new RecursoNoEncontradoException("Profesor no encontrado"));
        if(a!=null&&a.getUsuario()!=null || p!=null&&p.getUsuario()!=null) throw new ConflictoEntrenamientoException("El perfil ya tiene cuenta");
        String email=EmailNormalizer.normalize(a!=null?a.getEmail():p.getEmail());
        validacion.validar(new CambioEmail(email,0L,"Regularización de perfil"));
        // Incluye perfiles huérfanos y cuentas desactivadas en la reserva de identidad.
        if(usuarios.findByEmailIgnoreCase(email).isPresent()
                || alumnos.countEmailOcupado(email,-1L)>(esAlumno?1:0)
                || profesores.countEmailOcupado(email,-1L)>(esAlumno?0:1)) throw new ConflictoEntrenamientoException("Email ocupado por otra identidad");
        Usuario u=usuarios.saveAndFlush(Usuario.builder().email(email).password(encoder.encode(r.password())).rol(esAlumno?Rol.ALUMNO:Rol.PROFESOR).build());
        if(a!=null){a.setUsuario(u);a.setEmail(email);alumnos.saveAndFlush(a);u.setAlumno(a);}
        else {p.setUsuario(u);p.setEmail(email);profesores.saveAndFlush(p);u.setProfesor(p);}
        auditar(actor,u,"CREAR_CUENTA_PERFIL",r.motivo(),Map.of("perfilId",perfilId),AdminConsultaService.usuarioVista(u));
        return AdminConsultaService.usuarioVista(u);
    }
    private void validarPerfilRol(Rol rol,AlumnoDatos a,ProfesorDatos p,boolean existente) {
        if(rol==Rol.ADMIN && (a!=null||p!=null) || rol==Rol.ALUMNO && (p!=null||!existente&&a==null)
                || rol==Rol.PROFESOR && (a!=null||!existente&&p==null)) throw new ReglaNegocioException("Datos de perfil incompatibles con el rol solicitado");
    }
    private void aplicar(Alumno a,AlumnoDatos d) {
        int edad=d.edad();
        if(d.fechaNacimiento()!=null) {
            edad=Period.between(d.fechaNacimiento(),LocalDate.now()).getYears();
            if(edad<1||edad>150||edad!=d.edad()) throw new ReglaNegocioException("Edad y fecha de nacimiento incompatibles");
        }
        a.setNombre(d.nombre().trim());a.setApellido(d.apellido().trim());a.setEdad(edad);a.setFechaNacimiento(d.fechaNacimiento());
        a.setNivel(d.nivel());a.setObjetivo(d.objetivo());a.setEmailContacto(d.emailContacto());
    }
    private void aplicar(Profesor p,ProfesorDatos d) {
        p.setNombre(d.nombre().trim());p.setApellido(d.apellido().trim());p.setEspecialidad(d.especialidad().trim());p.setEmailContacto(d.emailContacto());
    }
    private void crearAlumno(Usuario u,AlumnoDatos d) {
        Alumno a=Alumno.builder().email(u.getEmail()).usuario(u).build();aplicar(a,d);alumnos.saveAndFlush(a);u.setAlumno(a);
    }
    private void crearProfesor(Usuario u,ProfesorDatos d) {
        Profesor p=Profesor.builder().email(u.getEmail()).usuario(u).build();aplicar(p,d);profesores.saveAndFlush(p);u.setProfesor(p);
    }
    private Usuario destino(Long id,long revision) {
        if(!usuarios.existsById(id)) throw new RecursoNoEncontradoException("Usuario no encontrado");
        Usuario u=identidad.bloquear(id);
        if(u.getTokenVersion()!=revision) throw new ConflictoEntrenamientoException("La cuenta cambió: recargá su estado");
        return u;
    }
    public UsuarioVista estado(Long id,CambioEstado r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);Usuario u=destino(id,r.revision());
        var antes=AdminConsultaService.usuarioVista(u);
        estados.cambiarEstado(id,r.activo(),auth);
        var despues=AdminConsultaService.usuarioVista(u);
        if(!antes.equals(despues)) auditar(actor,u,"CAMBIAR_ESTADO",r.motivo(),antes,despues);
        return despues;
    }
    public UsuarioVista email(Long id,CambioEmail r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);Usuario u=destino(id,r.revision());
        var antes=AdminConsultaService.usuarioVista(u);String email=EmailNormalizer.normalize(r.email());
        identidad.exigirEmailDisponible(email,u.getId());
        if(!email.equals(u.getEmail())) {
            u.setEmail(email);if(u.getAlumno()!=null)u.getAlumno().setEmail(email);if(u.getProfesor()!=null)u.getProfesor().setEmail(email);
            u.setTokenVersion(Math.incrementExact(u.getTokenVersion()));usuarios.saveAndFlush(u);
            auditar(actor,u,"CAMBIAR_EMAIL",r.motivo(),antes,AdminConsultaService.usuarioVista(u));
        }
        return AdminConsultaService.usuarioVista(u);
    }
    public UsuarioVista rol(Long id,CambioRol r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);Usuario u=destino(id,r.revision());
        boolean existe=r.rol()==Rol.ADMIN || r.rol()==Rol.ALUMNO&&u.getAlumno()!=null || r.rol()==Rol.PROFESOR&&u.getProfesor()!=null;
        validarPerfilRol(r.rol(),r.alumno(),r.profesor(),existe);
        if(r.rol()==u.getRol() && existe) {
            if(r.alumno()!=null||r.profesor()!=null) throw new ReglaNegocioException("Para editar un perfil existente usá su endpoint de perfil");
            return AdminConsultaService.usuarioVista(u);
        }
        if(u.isActivo()&&u.getRol()==Rol.ADMIN&&r.rol()!=Rol.ADMIN&&usuarios.countByRolAndActivoTrue(Rol.ADMIN)<=1)
            throw new ConflictoEntrenamientoException("No se puede cambiar el rol del último administrador activo");
        var antes=Map.of("cuenta",AdminConsultaService.usuarioVista(u),
                "profesoresAlumno",u.getAlumno()==null?List.of():u.getAlumno().getProfesores().stream().map(Profesor::getId).sorted().toList(),
                "alumnosProfesor",u.getProfesor()==null?List.of():alumnos.findDistinctByProfesores_IdOrderByIdAsc(u.getProfesor().getId()).stream().map(Alumno::getId).toList());
        // Conserva IDs y referencias históricas. Las relaciones de autorización se terminan, no se heredan al volver al rol.
        if(u.getRol()==Rol.ALUMNO && u.getAlumno()!=null) {
            Alumno a=alumnos.findByIdForUpdate(u.getAlumno().getId()).orElseThrow();a.getProfesores().clear();alumnos.flush();
        }
        if(u.getRol()==Rol.PROFESOR && u.getProfesor()!=null) {
            Long profesorId=u.getProfesor().getId();
            for(Alumno linked:alumnos.findDistinctByProfesores_IdOrderByIdAsc(profesorId)) {
                Alumno a=alumnos.findByIdForUpdate(linked.getId()).orElseThrow();a.getProfesores().removeIf(p->p.getId().equals(profesorId));
            }
            alumnos.flush();
        }
        if(r.rol()==Rol.ALUMNO && u.getAlumno()==null) crearAlumno(u,r.alumno());
        else if(r.alumno()!=null) aplicar(u.getAlumno(),r.alumno());
        if(r.rol()==Rol.PROFESOR && u.getProfesor()==null) crearProfesor(u,r.profesor());
        else if(r.profesor()!=null) aplicar(u.getProfesor(),r.profesor());
        u.setRol(r.rol());u.setTokenVersion(Math.incrementExact(u.getTokenVersion()));usuarios.saveAndFlush(u);
        auditar(actor,u,"CAMBIAR_ROL",r.motivo(),antes,AdminConsultaService.usuarioVista(u));
        return AdminConsultaService.usuarioVista(u);
    }
    public AlumnoVista alumno(Long id,AlumnoDatos r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);
        Alumno a=alumnos.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Alumno no encontrado"));
        if(a.getUsuario()!=null) identidad.bloquear(a.getUsuario().getId());
        a=alumnos.findByIdForUpdate(id).orElseThrow();var antes=consulta.alumnoVista(a);String snapshot=serializar(antes);
        aplicar(a,r);alumnos.saveAndFlush(a);var despues=consulta.alumnoVista(a);
        auditar(actor,a.getUsuario(),"EDITAR_ALUMNO","Edición administrativa de perfil",snapshot,serializar(despues));return despues;
    }
    public ProfesorVista profesor(Long id,ProfesorDatos r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);
        Profesor p=profesores.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Profesor no encontrado"));
        if(p.getUsuario()!=null) identidad.bloquear(p.getUsuario().getId());
        var antes=serializar(consulta.profesorVista(p));aplicar(p,r);profesores.saveAndFlush(p);var despues=consulta.profesorVista(p);
        auditar(actor,p.getUsuario(),"EDITAR_PROFESOR","Edición administrativa de perfil",antes,serializar(despues));return despues;
    }
    public AlumnoVista vinculo(Long alumnoId,Long profesorId,CambioVinculo r,boolean agregar,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);
        var antes=consulta.alumno(alumnoId,auth);
        if(agregar) alumnoService.elegirProfesor(alumnoId,profesorId,auth);else alumnoService.desvincularProfesor(alumnoId,profesorId,auth);
        var despues=consulta.alumno(alumnoId,auth);
        auditar(actor,despues.cuenta()==null?null:usuarios.getReferenceById(despues.cuenta().id()),agregar?"VINCULAR":"DESVINCULAR",r.motivo(),antes,despues);
        return despues;
    }
    public AlumnoVista reemplazar(Long alumnoId,ReemplazoVinculo r,Authentication auth) {
        Usuario actor=guard.bloquear(auth);validacion.validar(r);var antes=consulta.alumno(alumnoId,auth);
        alumnoService.reemplazarProfesor(alumnoId,r.profesorAnteriorId(),r.profesorNuevoId(),auth);
        var despues=consulta.alumno(alumnoId,auth);
        auditar(actor,despues.cuenta()==null?null:usuarios.getReferenceById(despues.cuenta().id()),"REEMPLAZAR_VINCULO",r.motivo(),antes,despues);return despues;
    }
    private String serializar(Object value) {
        try{return json.writeValueAsString(value);}catch(JsonProcessingException ex){throw new IllegalStateException("No se pudo registrar auditoría");}
    }
    private void auditar(Usuario actor,Usuario usuario,String accion,String motivo,Object antes,Object despues) {
        auditar(actor,usuario,accion,motivo,serializar(antes),serializar(despues));
    }
    private void auditar(Usuario actor,Usuario usuario,String accion,String motivo,String antes,String despues) {
        auditorias.saveAndFlush(AuditoriaAdministrativa.builder().actor(actor).usuario(usuario).accion(accion).motivo(motivo.trim())
                .antes(antes).despues(despues).creadaEn(LocalDateTime.now()).build());
    }
}
