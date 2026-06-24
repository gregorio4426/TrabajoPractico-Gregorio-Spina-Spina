package utn.simulacro_nombreAlumno.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.AsignacionMapper;
import utn.simulacro_nombreAlumno.mapper.RutinaMapper;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.AsignacionRutinaRequest;
import utn.simulacro_nombreAlumno.model.request.RutinaRequest;
import utn.simulacro_nombreAlumno.model.response.AsignacionResponse;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
import utn.simulacro_nombreAlumno.repository.AsignacionRutinaRepository;
import utn.simulacro_nombreAlumno.repository.RutinaRepository;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final RutinaMapper rutinaMapper;
    private final EjercicioService ejercicioService;
    private final ProfesorService profesorService;
    private final AsignacionRutinaRepository asignacionRutinaRepository;
    private final AlumnoService alumnoService;
    private final AsignacionMapper asignacionMapper;

    public Rutina findEntityById(long id) {
         return rutinaRepository.findById(id)
                 .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
     }

     public RutinaResponse findById(Long id) {
        return rutinaMapper.toDto(findEntityById(id));
    }

    public List<RutinaResponse> listarTodas (){
         List<Rutina> todas = rutinaRepository.findAll();
         return rutinaMapper.toLISTDto(todas);

     }

    public RutinaResponse crearRutina(RutinaRequest request, Authentication authentication) {
        if (request.getEjercicioIds() == null || request.getEjercicioIds().isEmpty()) {
            throw new ReglaNegocioException("La rutina debe tener al menos un ejercicio");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Profesor profesor = userDetails.getUsuario().getProfesor();
        if (profesor == null) throw new RecursoNoEncontradoException("No tenés un perfil de profesor");

        List<Ejercicio> ejercicios = new ArrayList<>();
        for (Long eid : request.getEjercicioIds()) {
            ejercicios.add(ejercicioService.findEntityById(eid));
        }
        Rutina rutina = rutinaMapper.toEntity(request);
        rutina.setEjercicios(ejercicios);
        rutina.setProfesor(profesor);
        return rutinaMapper.toDto(rutinaRepository.save(rutina));
    }

    public RutinaResponse updateRutina(Long id, RutinaRequest request, Authentication authentication) {
        Rutina rutina = findEntityById(id);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Profesor profesorAutenticado = userDetails.getUsuario().getProfesor();
        if (profesorAutenticado == null)
            throw new RecursoNoEncontradoException("No tenés un perfil de profesor");
        if (!rutina.getProfesor().getId().equals(profesorAutenticado.getId()))
            throw new org.springframework.security.access.AccessDeniedException("Solo el profesor que creó esta rutina puede modificarla");

        if (request.getEjercicioIds() == null || request.getEjercicioIds().isEmpty())
            throw new ReglaNegocioException("La rutina debe tener al menos un ejercicio");

        List<Ejercicio> ejercicios = new ArrayList<>();
        for (Long eid : request.getEjercicioIds()) {
            ejercicios.add(ejercicioService.findEntityById(eid));
        }
        rutina.setNombre(request.getNombre());
        rutina.setDescripcion(request.getDescripcion());
        rutina.setEjercicios(ejercicios);
        return rutinaMapper.toDto(rutinaRepository.save(rutina));
    }

    public void deleteRutina(Long id, Authentication authentication) {
        Rutina rutina = findEntityById(id);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Profesor profesorAutenticado = userDetails.getUsuario().getProfesor();
        if (profesorAutenticado == null)
            throw new RecursoNoEncontradoException("No tenés un perfil de profesor");
        if (!rutina.getProfesor().getId().equals(profesorAutenticado.getId()))
            throw new org.springframework.security.access.AccessDeniedException("Solo el profesor que creó esta rutina puede eliminarla");

        asignacionRutinaRepository.deleteAll(rutina.getAsignaciones());
        rutina.getEjercicios().clear();
        rutinaRepository.delete(rutina);
    }
    public AsignacionResponse asignarRutina(Long rutinaId, Long alumnoId) {
        Rutina rutina = findEntityById(rutinaId);
        Alumno alumno = alumnoService.findEntityById(alumnoId);


        asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno).ifPresent(a -> {
            a.setActiva(false);
            a.setFechaFin(LocalDateTime.now());
            asignacionRutinaRepository.save(a);
        });

        AsignacionRutina asignacion = AsignacionRutina.builder()
                .rutina(rutina)
                .alumno(alumno)
                .fechaInicio(LocalDateTime.now())
                .activa(true)
                .build();
        asignacionRutinaRepository.save(asignacion);

        return new AsignacionResponse(
                asignacion.getFechaInicio(),
                asignacion.getFechaFin(),
                asignacion.isActiva(),
                rutina.getNombre(),
                alumno.getNombre() + " " + alumno.getApellido()
        );
    }

    @Transactional
    public List<RutinaResponse> getHistorialDeAlumno(Long alumnoId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        boolean esProfesor = userDetails.getUsuario().getRol() == Rol.PROFESOR;
        if (!esProfesor && !userDetails.getUsuario().getAlumno().getId().equals(alumnoId)) {
            throw new AccessDeniedException("No tenés permiso para ver el historial de otro alumno");
        }
        Alumno alumno = alumnoService.findEntityById(alumnoId);
        List<AsignacionRutina> historial = asignacionRutinaRepository.findByAlumno(alumno);
        List<RutinaResponse> responses = new ArrayList<>();
        for (AsignacionRutina a : historial) {
            responses.add(rutinaMapper.toDto(a.getRutina()));
        }
        return responses;
    }

    @Transactional
    public RutinaResponse getRutinaActivaDeAlumno(Long alumnoId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        boolean esProfesor = userDetails.getUsuario().getRol() == Rol.PROFESOR;
        Alumno alumno = alumnoService.findEntityById(alumnoId);
        if (!esProfesor && !userDetails.getUsuario().getAlumno().getId().equals(alumnoId)) {
            throw new AccessDeniedException("No tenés permiso para ver las rutinas de otro alumno");
        }
        AsignacionRutina asignacion = asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno)
                .orElseThrow(() -> new RecursoNoEncontradoException("El alumno no tiene una rutina activa"));
        return rutinaMapper.toDto(asignacion.getRutina());
    }

    public AsignacionResponse asignarRutinaMe(Long alumnoId, Long rutinaId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails.getUsuario().getProfesor() == null)
            throw new RecursoNoEncontradoException("No tenés un perfil de profesor");

        Rutina rutina = findEntityById(rutinaId);
        Alumno alumno = alumnoService.findEntityById(alumnoId);

        asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno).ifPresent(a -> {
            a.setActiva(false);
            a.setFechaFin(LocalDateTime.now());
            asignacionRutinaRepository.save(a);
        });

        AsignacionRutina asignacion = AsignacionRutina.builder()
                .rutina(rutina)
                .alumno(alumno)
                .fechaInicio(LocalDateTime.now())
                .activa(true)
                .build();
        asignacionRutinaRepository.save(asignacion);
        return asignacionMapper.toResponse(asignacion);
    }

    public RutinaResponse getRutinaActivaMe(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (alumno == null) throw new RecursoNoEncontradoException("No tenés un perfil de alumno");
        AsignacionRutina asignacion = asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno)
                .orElseThrow(() -> new RecursoNoEncontradoException("No tenés una rutina activa"));
        return rutinaMapper.toDto(asignacion.getRutina());
    }

    public List<RutinaResponse> getHistorialMe(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (alumno == null) throw new RecursoNoEncontradoException("No tenés un perfil de alumno");
        List<AsignacionRutina> historial = asignacionRutinaRepository.findByAlumno(alumno);
        List<RutinaResponse> responses = new ArrayList<>();
        for (AsignacionRutina a : historial) {
            responses.add(rutinaMapper.toDto(a.getRutina()));
        }
        return responses;
    }
}


