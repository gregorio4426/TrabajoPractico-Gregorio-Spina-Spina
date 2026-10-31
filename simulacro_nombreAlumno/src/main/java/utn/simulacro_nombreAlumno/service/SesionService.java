package utn.simulacro_nombreAlumno.service;

import utn.simulacro_nombreAlumno.security.AccesoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import utn.simulacro_nombreAlumno.mapper.SesionMapper;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.request.*;
import utn.simulacro_nombreAlumno.exception.ConflictoEntrenamientoException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import utn.simulacro_nombreAlumno.model.response.AuditoriaEntrenamientoResponse;
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
    private final AccesoService acceso;
    private final utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento validacion;
    private final AuditoriaEntrenamientoRepository auditoriaRepository;
    private final ObjectMapper json;

    private Alumno alumnoAutenticado(Authentication authentication) {
        return acceso.alumnoPropio(authentication);
    }

    @Transactional
    public SesionResponse iniciarSesion(SesionRequest request, Authentication authentication) {
        acceso.alumnoPropio(authentication);
        validacion.validar(request);
        Alumno alumno = alumnoService.bloquearPropio(authentication);
        DiaRutina diaRutina = diaRutinaRepository.findById(request.getDiaRutinaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Día de rutina no encontrado"));

        AsignacionRutina activa = asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno)
                .orElseThrow(() -> new ReglaNegocioException("No tenés una rutina activa"));
        if (!diaRutina.getRutina().getId().equals(activa.getRutina().getId()))
            throw new AccessDeniedException("Ese día no pertenece a tu rutina activa");

        Sesion sesion = Sesion.builder()
                .alumno(alumno).diaRutina(diaRutina).asignacion(activa).estadoVinculoAsignacion("REGISTRADA")
                .fecha(request.getFecha()).completada(false).build();
        return sesionMapper.toDto(sesionRepository.save(sesion));
    }

    @Transactional
    public SerieResponse registrarSerie(Long sesionId, SerieRequest request, Authentication authentication) {
        Sesion sesion = bloquearPropia(sesionId, authentication);
        validacion.validar(request);
        if (sesion.isCompletada()) throw new ReglaNegocioException("La sesión ya está completada");

        DiaEjercicio diaEjercicio = diaEjercicioRepository.findById(request.getDiaEjercicioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ejercicio del día no encontrado"));
        if (!diaEjercicio.getDiaRutina().getId().equals(sesion.getDiaRutina().getId()))
            throw new AccessDeniedException("Ese ejercicio no pertenece al día que estás entrenando");

        exigirNumeroDisponible(sesionId, diaEjercicio.getEjercicio().getId(), request.getNumeroSerie(), -1L);
        Serie serie = Serie.builder().sesion(sesion).diaEjercicio(diaEjercicio).ejercicio(diaEjercicio.getEjercicio())
                .numeroSerie(request.getNumeroSerie()).repsHechas(request.getRepsHechas())
                .pesoUsado(request.getPesoUsado()).completada(true).build();
        Serie guardada = serieRepository.saveAndFlush(serie);
        tocar(sesion);
        return sesionMapper.toSerieDto(guardada);
    }

    @Transactional
    public SesionResponse completarSesion(Long sesionId, Authentication authentication) {
        Sesion sesion = bloquearPropia(sesionId, authentication);
        if (sesion.isCompletada()) throw new ReglaNegocioException("La sesión ya estaba completada");
        if (sesion.getSeries().stream().noneMatch(Serie::isVigenteValida)) {
            throw new ReglaNegocioException("No se puede finalizar una sesión sin series válidas vigentes");
        }
        sesion.setCompletada(true);
        sesion.setFinalizadaEn(LocalDateTime.now());
        tocar(sesion);
        return sesionMapper.toDto(sesion);
    }

    @Transactional
    public SesionResponse getDetalle(Long sesionId, Authentication authentication) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        acceso.leerAlumno(sesion.getAlumno().getId(), authentication);
        return sesionMapper.toDto(sesion);
    }

    @Transactional
    public List<SesionResponse> getHistorialMe(Authentication authentication) {
        Alumno alumno = alumnoAutenticado(authentication);
        return sesionMapper.toLISTDto(sesionRepository.findByAlumnoOrderByFechaDesc(alumno));
    }

    @Transactional
    public List<SesionResponse> getHistorialDeAlumno(Long alumnoId, Authentication authentication) {
        acceso.gestionarAlumno(alumnoId, authentication);
        Alumno alumno = alumnoService.findEntityById(alumnoId);
        return sesionMapper.toLISTDto(sesionRepository.findByAlumnoOrderByFechaDesc(alumno));
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
                .sesionId(ultimaSesion.getId()).fecha(ultimaSesion.getFecha()).fechaEfectiva(ultimaSesion.getFechaEfectiva())
                .ejercicioId(ejercicio.getId()).ejercicioNombre(series.get(0).getDiaEjercicio().getNombreHistorico())
                .series(seriesUltimaSesion.stream().map(sesionMapper::toSerieDto).toList()).build();
    }

    @Transactional
    public SerieResponse corregirSerie(Long sesionId, Long serieId, CorreccionSerieRequest request, Authentication auth) {
        return cambiarSerie(sesionId, serieId, request, auth, false);
    }

    @Transactional
    public SerieResponse restaurarSerie(Long sesionId, Long serieId, CorreccionSerieRequest request, Authentication auth) {
        return cambiarSerie(sesionId, serieId, request, auth, true);
    }

    private SerieResponse cambiarSerie(Long sesionId, Long serieId, CorreccionSerieRequest request, Authentication auth, boolean restaurar) {
        Sesion sesion = bloquearPropia(sesionId, auth);
        validacion.validar(request);
        Serie serie = serieDeSesion(sesionId, serieId);
        exigirRevision(serie.getRevision(), request.getRevision());
        if (!restaurar && serie.isAnulada()) throw new ReglaNegocioException("Para una serie anulada usá la restauración explícita");
        if (restaurar && !serie.isAnulada()) throw new ReglaNegocioException("La serie no está anulada");
        exigirNumeroDisponible(sesionId, serie.getDiaEjercicio().getEjercicio().getId(), request.getNumeroSerie(), serieId);
        String antes = fotoSerie(serie);
        serie.setNumeroSerie(request.getNumeroSerie());
        serie.setRepsHechas(request.getRepsHechas());
        serie.setPesoUsado(request.getPesoUsado());
        serie.setCompletada(true);
        if (restaurar) serie.setAnulada(false);
        serie.setLegadoExento(false); // La evidencia anterior permanece en la auditoría y la incidencia original.
        serieRepository.saveAndFlush(serie);
        tocar(sesion);
        auditar(sesion, serie, auth, restaurar ? "RESTAURAR_SERIE" : "CORREGIR_SERIE", request.getMotivo(), antes, fotoSerie(serie));
        return sesionMapper.toSerieDto(serie);
    }

    @Transactional
    public SerieResponse anularSerie(Long sesionId, Long serieId, AnulacionSerieRequest request, Authentication auth) {
        Sesion sesion = bloquearPropia(sesionId, auth);
        validacion.validar(request);
        Serie serie = serieDeSesion(sesionId, serieId);
        exigirRevision(serie.getRevision(), request.getRevision());
        if (serie.isAnulada()) throw new ReglaNegocioException("La serie ya está anulada");
        if (sesion.isCompletada() && sesion.getSeries().stream().filter(s -> !s.getId().equals(serieId)).noneMatch(Serie::isVigenteValida)) {
            throw new ReglaNegocioException("Una sesión finalizada no puede quedar sin series válidas");
        }
        String antes = fotoSerie(serie);
        serie.setAnulada(true);
        serieRepository.saveAndFlush(serie);
        tocar(sesion);
        auditar(sesion, serie, auth, "ANULAR_SERIE", request.getMotivo(), antes, fotoSerie(serie));
        return sesionMapper.toSerieDto(serie);
    }

    @Transactional
    public SesionResponse corregirSesion(Long sesionId, CorreccionSesionRequest request, Authentication auth) {
        Sesion sesion = bloquearPropia(sesionId, auth);
        validacion.validar(request);
        exigirRevision(sesion.getRevision(), request.getRevision());
        String antes = fotoSesion(sesion);
        sesion.setFechaCorregida(request.getFecha()); // La fecha original es inmutable.
        tocar(sesion);
        auditar(sesion, null, auth, "CORREGIR_FECHA", request.getMotivo(), antes, fotoSesion(sesion));
        return sesionMapper.toDto(sesion);
    }

    @Transactional
    public List<AuditoriaEntrenamientoResponse> auditoria(Long sesionId, Authentication auth) {
        Sesion sesion = sesionRepository.findById(sesionId).orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        acceso.leerAlumno(sesion.getAlumno().getId(), auth);
        return auditoriaRepository.findBySesion_IdOrderByIdAsc(sesionId).stream().map(a -> new AuditoriaEntrenamientoResponse(
                a.getId(), sesionId, a.getSerie() == null ? null : a.getSerie().getId(), a.getActor().getId(), a.getAccion(),
                a.getMotivo(), leerJson(a.getAntes()), leerJson(a.getDespues()), a.getCreadaEn())).toList();
    }

    private Sesion bloquearPropia(Long sesionId, Authentication auth) {
        Alumno alumno = alumnoAutenticado(auth);
        Sesion sesion = sesionRepository.findByIdForUpdate(sesionId).orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));
        if (!sesion.getAlumno().getId().equals(alumno.getId())) throw new AccessDeniedException("La sesión no es tuya");
        return sesion;
    }

    private Serie serieDeSesion(Long sesionId, Long serieId) {
        return serieRepository.findById(serieId).filter(s -> s.getSesion().getId().equals(sesionId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Serie no encontrada en esta sesión"));
    }

    private void exigirNumeroDisponible(Long sesionId, Long ejercicioId, Integer numero, Long excluir) {
        if (serieRepository.existsBySesion_IdAndDiaEjercicio_Ejercicio_IdAndNumeroSerieAndAnuladaFalseAndIdNot(sesionId, ejercicioId, numero, excluir)) {
            throw new ConflictoEntrenamientoException("Número de serie ya registrado para este ejercicio en la sesión");
        }
    }

    private void exigirRevision(long actual, Long esperada) {
        if (esperada == null || actual != esperada) throw new ConflictoEntrenamientoException("El registro cambió; recargá antes de corregir");
    }

    private void tocar(Sesion sesion) {
        sesion.setActualizadaEn(LocalDateTime.now());
        sesionRepository.saveAndFlush(sesion);
    }

    private void auditar(Sesion sesion, Serie serie, Authentication auth, String accion, String motivo, String antes, String despues) {
        auditoriaRepository.saveAndFlush(AuditoriaEntrenamiento.builder().sesion(sesion).serie(serie).actor(acceso.usuario(auth))
                .accion(accion).motivo(motivo).antes(antes).despues(despues).creadaEn(LocalDateTime.now()).build());
    }

    private String fotoSerie(Serie serie) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("serieId", serie.getId()); datos.put("sesionId", serie.getSesion().getId());
        datos.put("ejercicioId", serie.getDiaEjercicio().getEjercicio().getId()); datos.put("diaEjercicioId", serie.getDiaEjercicio().getId());
        datos.put("rutinaVersionId", serie.getSesion().getDiaRutina().getRutina().getId());
        datos.put("numeroSerie", serie.getNumeroSerie()); datos.put("repsHechas", serie.getRepsHechas()); datos.put("pesoUsado", serie.getPesoUsado());
        datos.put("completada", serie.isCompletada());
        datos.put("anulada", serie.isAnulada()); datos.put("legadoExento", serie.isLegadoExento());
        datos.put("incidenciaLegado", serie.getIncidenciaLegado()); datos.put("revision", serie.getRevision());
        datos.put("fechaOriginal", serie.getSesion().getFecha()); datos.put("fechaEfectiva", serie.getSesion().getFechaEfectiva());
        return escribirJson(datos);
    }

    private String fotoSesion(Sesion sesion) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("sesionId", sesion.getId()); datos.put("fechaOriginal", sesion.getFecha()); datos.put("fechaEfectiva", sesion.getFechaEfectiva());
        datos.put("revision", sesion.getRevision()); datos.put("finalizada", sesion.isCompletada());
        datos.put("asignacionId", sesion.getAsignacion() == null ? null : sesion.getAsignacion().getId());
        datos.put("rutinaVersionId", sesion.getDiaRutina().getRutina().getId());
        return escribirJson(datos);
    }

    private String escribirJson(Object value) {
        try { return json.writeValueAsString(value); } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo registrar la auditoría", ex);
        }
    }
    private com.fasterxml.jackson.databind.JsonNode leerJson(String value) {
        try { return json.readTree(value); } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Auditoría ilegible", ex);
        }
    }
}
