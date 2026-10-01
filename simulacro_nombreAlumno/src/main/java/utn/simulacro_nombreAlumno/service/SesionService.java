package utn.simulacro_nombreAlumno.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import utn.simulacro_nombreAlumno.mapper.SesionMapper;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.SerieRequest;
import utn.simulacro_nombreAlumno.model.request.SesionRequest;
import utn.simulacro_nombreAlumno.model.response.SerieResponse;
import utn.simulacro_nombreAlumno.model.response.SesionResponse;
import utn.simulacro_nombreAlumno.model.response.UltimoEjercicioResponse;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SesionService {
    private final SesionRepository sesionRepository;
    private final SerieRepository serieRepository;
    private final DiaRutinaRepository diaRutinaRepository;
    private final DiaEjercicioRepository diaEjercicioRepository;
    private final AsignacionRutinaRepository asignacionRutinaRepository;
    private final SesionMapper sesionMapper;
    private final AlumnoService alumnoService;

    private CustomUserDetails principal(Authentication authentication) {
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private Alumno alumnoAutenticado(Authentication authentication) {
        Alumno alumno = principal(authentication).getUsuario().getAlumno();
        if (alumno == null) throw new AccessDeniedException("Esta operación requiere un perfil de alumno");
        return alumno;
    }

    @Transactional
    public SesionResponse iniciarSesion(SesionRequest request, Authentication authentication) {
        Alumno alumno = alumnoAutenticado(authentication);
        DiaRutina diaRutina = diaRutinaRepository.findById(request.getDiaRutinaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Día de rutina no encontrado"));

        AsignacionRutina activa = asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno)
                .orElseThrow(() -> new ReglaNegocioException("No tenés una rutina activa"));
        if (!diaRutina.getRutina().getId().equals(activa.getRutina().getId()))
            throw new AccessDeniedException("Ese día no pertenece a tu rutina activa");

        Sesion sesion = Sesion.builder()
                .alumno(alumno).diaRutina(diaRutina).fecha(request.getFecha()).completada(false).build();
        return sesionMapper.toDto(sesionRepository.save(sesion));
    }

    @Transactional
    public SerieResponse registrarSerie(Long sesionId, SerieRequest request, Authentication authentication) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        Alumno alumno = alumnoAutenticado(authentication);
        if (!sesion.getAlumno().getId().equals(alumno.getId()))
            throw new AccessDeniedException("No podés registrar series en una sesión que no es tuya");
        if (sesion.isCompletada())
            throw new ReglaNegocioException("La sesión ya está completada");

        DiaEjercicio diaEjercicio = diaEjercicioRepository.findById(request.getDiaEjercicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ejercicio del día no encontrado"));
        if (!diaEjercicio.getDiaRutina().getId().equals(sesion.getDiaRutina().getId()))
            throw new AccessDeniedException("Ese ejercicio no pertenece al día que estás entrenando");

        Serie serie = Serie.builder().sesion(sesion).diaEjercicio(diaEjercicio)
                .numeroSerie(request.getNumeroSerie()).repsHechas(request.getRepsHechas())
                .pesoUsado(request.getPesoUsado()).completada(true).build();
        return sesionMapper.toSerieDto(serieRepository.save(serie));
    }

    @Transactional
    public SesionResponse completarSesion(Long sesionId, Authentication authentication) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        Alumno alumno = alumnoAutenticado(authentication);
        if (!sesion.getAlumno().getId().equals(alumno.getId()))
            throw new AccessDeniedException("No podés completar una sesión que no es tuya");
        if (sesion.isCompletada()) throw new ReglaNegocioException("La sesión ya estaba completada");
        sesion.setCompletada(true);
        return sesionMapper.toDto(sesionRepository.save(sesion));
    }

    @Transactional
    public SesionResponse getDetalle(Long sesionId, Authentication authentication) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        CustomUserDetails user = principal(authentication);
        if (user.getUsuario().getRol() == Rol.ALUMNO) {
            Alumno alumno = alumnoAutenticado(authentication);
            if (!sesion.getAlumno().getId().equals(alumno.getId()))
                throw new AccessDeniedException("No podés ver una sesión que no es tuya");
        } else if (user.getUsuario().getRol() == Rol.PROFESOR) {
            Profesor profesor = user.getUsuario().getProfesor();
            if (profesor == null || !sesion.getDiaRutina().getRutina().getProfesor().getId().equals(profesor.getId()))
                throw new AccessDeniedException("Solo podés ver sesiones correspondientes a tus rutinas");
        } else {
            throw new AccessDeniedException("No tenés permiso para consultar esta sesión");
        }
        return sesionMapper.toDto(sesion);
    }

    @Transactional
    public List<SesionResponse> getHistorialMe(Authentication authentication) {
        Alumno alumno = alumnoAutenticado(authentication);
        return sesionMapper.toLISTDto(sesionRepository.findByAlumnoOrderByFechaDesc(alumno));
    }

    @Transactional
    public List<SesionResponse> getHistorialDeAlumno(Long alumnoId, Authentication authentication) {
        CustomUserDetails user = principal(authentication);
        Profesor profesor = user.getUsuario().getProfesor();
        if (user.getUsuario().getRol() != Rol.PROFESOR || profesor == null)
            throw new AccessDeniedException("Solo un profesor puede ver el historial de un alumno");
        Alumno alumno = alumnoService.findEntityById(alumnoId);
        List<Sesion> sesiones = sesionRepository.findByAlumnoAndDiaRutina_Rutina_ProfesorOrderByFechaDesc(alumno, profesor);
        if (sesiones.isEmpty())
            throw new AccessDeniedException("El alumno no tiene sesiones correspondientes a tus rutinas");
        return sesionMapper.toLISTDto(sesiones);
    }

    @Transactional
    public UltimoEjercicioResponse getUltimoEjercicio(Long ejercicioId, Authentication authentication) {
        Alumno alumno = alumnoAutenticado(authentication);
        List<Serie> series = serieRepository
                .findBySesion_AlumnoAndDiaEjercicio_Ejercicio_IdAndSesion_CompletadaTrueOrderBySesion_FechaDescIdDesc(alumno, ejercicioId);
        if (series.isEmpty()) throw new RecursoNoEncontradoException("Todavía no registraste este ejercicio en una sesión completada");

        Sesion ultimaSesion = series.get(0).getSesion();
        List<Serie> seriesUltimaSesion = serieRepository
                .findBySesion_IdAndDiaEjercicio_Ejercicio_IdOrderByNumeroSerieAsc(ultimaSesion.getId(), ejercicioId);
        Ejercicio ejercicio = series.get(0).getDiaEjercicio().getEjercicio();
        return UltimoEjercicioResponse.builder()
                .sesionId(ultimaSesion.getId()).fecha(ultimaSesion.getFecha())
                .ejercicioId(ejercicio.getId()).ejercicioNombre(ejercicio.getNombre())
                .series(seriesUltimaSesion.stream().map(sesionMapper::toSerieDto).toList()).build();
    }
}
