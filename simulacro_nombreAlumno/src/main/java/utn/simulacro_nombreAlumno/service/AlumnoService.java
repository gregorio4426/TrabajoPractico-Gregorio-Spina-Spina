package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.*;
import utn.simulacro_nombreAlumno.mapper.AlumnoMapper;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.*;
import utn.simulacro_nombreAlumno.model.response.AlumnoResponse;
import utn.simulacro_nombreAlumno.repository.AlumnoRepository;
import utn.simulacro_nombreAlumno.security.AccesoService;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AlumnoService {
    private final AlumnoMapper alumnoMapper;
    private final AlumnoRepository alumnoRepository;
    private final ProfesorService profesorService;
    private final AccesoService acceso;
    private final IdentidadService identidad;

    public Alumno createAlumno(RegisterAlumnoRequest request, Usuario usuario) {
        return alumnoRepository.save(Alumno.builder().nombre(request.getNombre()).apellido(request.getApellido())
                .email(request.getEmail()).edad(request.getEdad()).fechaNacimiento(request.getFechaNacimiento())
                .nivel(request.getNivel()).objetivo(request.getObjetivo()).usuario(usuario).build());
    }

    @Transactional
    public AlumnoResponse updateAlumno(Long id, AlumnoRequest request, Authentication auth) {
        acceso.editarPerfilAlumno(id, auth);
        Alumno alumno = findEntityById(id);
        aplicarPerfil(alumno, request);
        return alumnoMapper.toDto(alumnoRepository.save(alumno));
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponse> listarAlumnos() {
        Authentication auth = actual();
        Usuario usuario = acceso.exigirRol(auth, Rol.ADMIN, Rol.PROFESOR);
        List<Alumno> alumnos = usuario.getRol() == Rol.ADMIN ? alumnoRepository.findAll()
                : alumnoRepository.findDistinctByProfesores_IdOrderByIdAsc(acceso.profesorPropio(auth).getId());
        return alumnoMapper.toLISTDto(alumnos);
    }

    @Transactional(readOnly = true)
    public List<AlumnoResponse> misAlumnos(Authentication auth) {
        Long profesorId = acceso.profesorPropio(auth).getId();
        return alumnoMapper.toLISTDto(alumnoRepository.findDistinctByProfesores_IdOrderByIdAsc(profesorId));
    }

    @Transactional
    public AlumnoResponse patchNivel(Long id, Nivel nivel) {
        Alumno alumno = gestionarEntity(id, actual());
        if (nivel == null) throw new ReglaNegocioException("Nivel requerido");
        alumno.setNivel(nivel);
        return alumnoMapper.toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoResponse patchObjetivo(Long id, Objetivo objetivo) {
        Alumno alumno = gestionarEntity(id, actual());
        if (objetivo == null) throw new ReglaNegocioException("Objetivo requerido");
        alumno.setObjetivo(objetivo);
        return alumnoMapper.toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoResponse actualizarSeguimiento(Long id, SeguimientoAlumnoRequest request, Authentication auth) {
        Alumno alumno = gestionarEntity(id, auth);
        if (request.getNivel() == null || request.getObjetivo() == null) {
            throw new ReglaNegocioException("Nivel y objetivo requeridos");
        }
        alumno.setNivel(request.getNivel());
        alumno.setObjetivo(request.getObjetivo());
        return alumnoMapper.toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoResponse elegirProfesor(Long alumnoId, Long profesorId, Authentication auth) {
        acceso.editarPerfilAlumno(alumnoId, auth);
        Profesor profesor = profesorVinculable(profesorId);
        Alumno alumno = bloquear(alumnoId);
        exigirAlumnoVigente(alumno);
        if (alumno.getProfesores().stream().noneMatch(p -> p.getId().equals(profesorId))) {
            alumno.getProfesores().add(profesor);
        }
        return alumnoMapper.toDto(alumnoRepository.save(alumno));
    }

    @Transactional
    public AlumnoResponse desvincularProfesor(Long alumnoId, Long profesorId, Authentication auth) {
        acceso.editarPerfilAlumno(alumnoId, auth);
        return quitarVinculo(alumnoId, profesorId);
    }

    @Transactional
    public void desvincularMiAlumno(Long alumnoId, Authentication auth) {
        Long profesorId = acceso.profesorPropio(auth).getId();
        acceso.gestionarAlumno(alumnoId, auth);
        quitarVinculo(alumnoId, profesorId);
    }

    @Transactional
    public AlumnoResponse reemplazarProfesor(Long alumnoId, Long anteriorId, Long nuevoId, Authentication auth) {
        acceso.editarPerfilAlumno(alumnoId, auth);
        Profesor nuevo = profesorVinculable(nuevoId);
        Alumno alumno = bloquear(alumnoId);
        exigirAlumnoVigente(alumno);
        if (alumno.getProfesores().stream().noneMatch(p -> p.getId().equals(anteriorId))) {
            throw new ReglaNegocioException("El profesor anterior no está vinculado");
        }
        if (!anteriorId.equals(nuevoId)) {
            alumno.getProfesores().removeIf(p -> p.getId().equals(anteriorId));
            if (alumno.getProfesores().stream().noneMatch(p -> p.getId().equals(nuevoId))) {
                alumno.getProfesores().add(nuevo);
            }
        }
        return alumnoMapper.toDto(alumnoRepository.save(alumno));
    }

    private Profesor profesorVinculable(Long id) {
        Profesor p = profesorService.findEntityById(id);
        if (p.getUsuario() == null) throw new ConflictoEntrenamientoException("El profesor necesita una cuenta vigente");
        Usuario u = identidad.bloquear(p.getUsuario().getId());
        if (!u.isActivo() || u.getRol() != Rol.PROFESOR) throw new ConflictoEntrenamientoException("El profesor no está habilitado");
        return p;
    }

    private void exigirAlumnoVigente(Alumno alumno) {
        if (alumno.getUsuario() == null || alumno.getUsuario().getRol() != Rol.ALUMNO)
            throw new ConflictoEntrenamientoException("Perfil de alumno sin cuenta de alumno vigente");
    }

    private AlumnoResponse quitarVinculo(Long alumnoId, Long profesorId) {
        Alumno alumno = bloquear(alumnoId);
        profesorService.findEntityById(profesorId);
        alumno.getProfesores().removeIf(p -> p.getId().equals(profesorId));
        // No modifica asignaciones, rutinas, sesiones ni series.
        return alumnoMapper.toDto(alumnoRepository.save(alumno));
    }

    /** Bloqueo compartido con la gestión de vínculos antes de autorizar escrituras. */
    @Transactional
    public Alumno gestionarEntity(Long id, Authentication auth) {
        acceso.exigirRol(auth, Rol.ADMIN, Rol.PROFESOR);
        Alumno alumno = bloquear(id);
        acceso.gestionarAlumno(id, auth);
        return alumno;
    }

    @Transactional
    public Alumno bloquearPropio(Authentication auth) {
        return bloquear(acceso.alumnoPropio(auth).getId());
    }

    private Alumno bloquear(Long id) {
        return alumnoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }

    /** Búsqueda interna para colaboración entre services; no exponer como endpoint. */
    public Alumno findEntityById(long id) {
        return alumnoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
    }

    @Transactional(readOnly = true)
    public AlumnoResponse findAlumnoResponseById(long id) {
        acceso.leerAlumno(id, actual());
        return alumnoMapper.toDto(findEntityById(id));
    }

    public Optional<Alumno> findByUsuario(Usuario usuario) {
        return alumnoRepository.findByUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public AlumnoResponse getMiPerfil(Authentication auth) {
        return alumnoMapper.toDto(findEntityById(acceso.alumnoPropio(auth).getId()));
    }

    @Transactional
    public AlumnoResponse updateMiPerfil(AlumnoRequest request, Authentication auth) {
        return updateAlumno(acceso.alumnoPropio(auth).getId(), request, auth);
    }

    @Transactional
    public AlumnoResponse elegirProfesorMe(Long profesorId, Authentication auth) {
        return elegirProfesor(acceso.alumnoPropio(auth).getId(), profesorId, auth);
    }

    @Transactional
    public AlumnoResponse desvincularProfesorMe(Long profesorId, Authentication auth) {
        return desvincularProfesor(acceso.alumnoPropio(auth).getId(), profesorId, auth);
    }

    private Authentication actual() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private void aplicarPerfil(Alumno alumno, AlumnoRequest request) {
        String email = identidad.correoPerfil(alumno.getUsuario(), request.getEmail());
        alumno.setNombre(request.getNombre());
        alumno.setApellido(request.getApellido());
        if (email != null) alumno.setEmail(email);
        if (request.getEmailContacto() != null) alumno.setEmailContacto(request.getEmailContacto());
        if (request.getFechaDeNacimiento() != null) alumno.setFechaNacimiento(request.getFechaDeNacimiento());
        if (alumno.getFechaNacimiento() != null) {
            alumno.setEdad(Period.between(alumno.getFechaNacimiento(), LocalDate.now()).getYears());
        }
        alumno.setNivel(request.getNivel());
        alumno.setObjetivo(request.getObjetivo());
    }
}
