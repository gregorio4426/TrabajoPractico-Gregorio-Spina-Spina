package utn.simulacro_nombreAlumno.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.SesionMapper;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.SerieRequest;
import utn.simulacro_nombreAlumno.model.request.SesionRequest;
import utn.simulacro_nombreAlumno.model.response.SerieResponse;
import utn.simulacro_nombreAlumno.model.response.SesionResponse;
import utn.simulacro_nombreAlumno.repository.DiaEjercicioRepository;
import utn.simulacro_nombreAlumno.repository.DiaRutinaRepository;
import utn.simulacro_nombreAlumno.repository.SerieRepository;
import utn.simulacro_nombreAlumno.repository.SesionRepository;
import utn.simulacro_nombreAlumno.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SesionService {

    private final SesionRepository sesionRepository;
    private final SerieRepository serieRepository;
    private final DiaRutinaRepository diaRutinaRepository;
    private final DiaEjercicioRepository diaEjercicioRepository;
    private final SesionMapper sesionMapper;
    private final AlumnoService alumnoService;


    @Transactional
    public SesionResponse iniciarSesion(SesionRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (alumno == null) throw new RecursoNoEncontradoException("No tenés un perfil de alumno");

        DiaRutina diaRutina = diaRutinaRepository.findById(request.getDiaRutinaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Día de rutina no encontrado"));

        Sesion sesion = Sesion.builder()
                .alumno(alumno)
                .diaRutina(diaRutina)
                .fecha(request.getFecha())
                .completada(false)
                .build();

        return sesionMapper.toDto(sesionRepository.save(sesion));
    }


    @Transactional
    public SerieResponse registrarSerie(Long sesionId, SerieRequest request, Authentication authentication) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (!sesion.getAlumno().getId().equals(alumno.getId()))
            throw new AccessDeniedException("No podés registrar series en una sesión que no es tuya");

        DiaEjercicio diaEjercicio = diaEjercicioRepository.findById(request.getDiaEjercicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ejercicio del día no encontrado"));

        Serie serie = Serie.builder()
                .sesion(sesion)
                .diaEjercicio(diaEjercicio)
                .numeroSerie(request.getNumeroSerie())
                .repsHechas(request.getRepsHechas())
                .pesoUsado(request.getPesoUsado())
                .completada(true)
                .build();

        return sesionMapper.toSerieDto(serieRepository.save(serie));
    }


    @Transactional
    public SesionResponse completarSesion(Long sesionId, Authentication authentication) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (!sesion.getAlumno().getId().equals(alumno.getId()))
            throw new AccessDeniedException("No podés completar una sesión que no es tuya");

        sesion.setCompletada(true);
        return sesionMapper.toDto(sesionRepository.save(sesion));
    }


    @Transactional
    public List<SesionResponse> getHistorialMe(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Alumno alumno = userDetails.getUsuario().getAlumno();
        if (alumno == null) throw new RecursoNoEncontradoException("No tenés un perfil de alumno");
        return sesionMapper.toLISTDto(sesionRepository.findByAlumnoOrderByFechaDesc(alumno));
    }


    @Transactional
    public List<SesionResponse> getHistorialDeAlumno(Long alumnoId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        boolean esProfesor = userDetails.getUsuario().getRol() == Rol.PROFESOR;
        if (!esProfesor)
            throw new AccessDeniedException("Solo un profesor puede ver el historial de otro alumno");
        Alumno alumno = alumnoService.findEntityById(alumnoId);
        return sesionMapper.toLISTDto(sesionRepository.findByAlumnoOrderByFechaDesc(alumno));
    }
}