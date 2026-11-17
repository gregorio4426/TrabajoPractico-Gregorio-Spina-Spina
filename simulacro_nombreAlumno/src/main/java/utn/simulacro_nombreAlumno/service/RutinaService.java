package utn.simulacro_nombreAlumno.service;

import utn.simulacro_nombreAlumno.security.AccesoService;
import org.springframework.security.core.context.SecurityContextHolder;
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
import utn.simulacro_nombreAlumno.model.DiaRutina;
import utn.simulacro_nombreAlumno.model.DiaEjercicio;

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
    private final AccesoService acceso;
    private final utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento validacion;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public Rutina findEntityById(long id) {
         return rutinaRepository.findById(id)
                 .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
     }

    @Transactional
    public RutinaResponse findById(Long id) {
        acceso.usuario(actual());
        return respuesta(ultima(raiz(findEntityById(id))), actual());
    }

    @Transactional
    public List<RutinaResponse> listarTodas() {
        acceso.usuario(actual());
        return rutinaRepository.findByRaizIsNullAndArchivadaFalseOrderByIdAsc().stream()
                .map(this::ultima).map(r -> respuesta(r, actual())).toList();
    }

    @Transactional
    public List<RutinaResponse> listarVersiones(Long id, Authentication auth) {
        acceso.usuario(auth);
        Rutina raiz = raiz(findEntityById(id));
        List<Rutina> versiones = new ArrayList<>();
        versiones.add(raiz);
        versiones.addAll(rutinaRepository.findByRaiz_IdOrderByNumeroVersionAsc(raiz.getId()));
        return versiones.stream().map(r -> respuesta(r, auth)).toList();
    }

    @Transactional
    public RutinaResponse getVersion(Long id, int numero, Authentication auth) {
        acceso.usuario(auth);
        Rutina raiz = raiz(findEntityById(id));
        Rutina version = numero == 1 ? raiz : rutinaRepository.findByRaiz_IdAndNumeroVersion(raiz.getId(), numero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión de rutina no encontrada"));
        return respuesta(version, auth);
    }

    @Transactional
    public RutinaResponse crearRutina(RutinaRequest request, Authentication auth) {
        Profesor profesor = acceso.profesorPropio(auth);
        validacion.rutina(request, true);
        if (request.getDias() == null || request.getDias().isEmpty()) {
            throw new ReglaNegocioException("La rutina debe tener al menos un día");
        }
        Rutina rutina = Rutina.builder().nombre(request.getNombre()).descripcion(request.getDescripcion())
                .profesor(profesor).build();
        cargarDias(rutina, request);
        return respuesta(rutinaRepository.save(rutina), auth);
    }

    @Transactional
    public RutinaResponse updateRutina(Long id, RutinaRequest request, Authentication auth) {
        Profesor profesor = acceso.profesorPropio(auth);
        Rutina raiz = raizBloqueada(id);
        exigirCreador(raiz, profesor);
        validacion.rutina(request, false);
        if (raiz.isArchivada()) throw new ReglaNegocioException("La rutina está archivada");
        Rutina anterior = ultimaBloqueada(raiz);
        Rutina nueva = Rutina.builder().raiz(raiz).numeroVersion(anterior.getNumeroVersion() + 1)
                .nombre(request.getNombre()).descripcion(request.getDescripcion()).profesor(raiz.getProfesor()).build();
        if (request.getDias() == null || request.getDias().isEmpty()) {
            copiarDias(anterior, nueva); // Compatibilidad con PUT de metadatos sin días.
        } else cargarDias(nueva, request);
        if (java.util.Objects.equals(anterior.getNombre(), nueva.getNombre())
                && java.util.Objects.equals(anterior.getDescripcion(), nueva.getDescripcion())
                && firma(anterior).equals(firma(nueva))) return respuesta(anterior, auth);
        return respuesta(rutinaRepository.save(nueva), auth);
    }

    @Transactional
    public void deleteRutina(Long id, Authentication auth) {
        Profesor profesor = acceso.profesorPropio(auth);
        Rutina raiz = raizBloqueada(id);
        exigirCreador(raiz, profesor);
        raiz.setArchivada(true); // Ningún DELETE ni limpieza de relaciones históricas.
    }

    private Rutina raiz(Rutina rutina) { return rutina.getRaiz() == null ? rutina : rutina.getRaiz(); }

    private Rutina raizBloqueada(Long id) {
        Rutina raiz = raiz(findEntityById(id));
        // Refresca también el primer nivel de caché tras esperar el bloqueo.
        entityManager.refresh(raiz, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return raiz;
    }

    private Rutina ultimaBloqueada(Rutina raiz) {
        return rutinaRepository.findTopByRaiz_IdOrderByNumeroVersionDesc(raiz.getId()).orElse(raiz);
    }

    private Rutina ultima(Rutina raiz) {
        return rutinaRepository.findFirstByRaiz_IdOrderByNumeroVersionDesc(raiz.getId()).orElse(raiz);
    }

    private void exigirCreador(Rutina rutina, Profesor profesor) {
        if (rutina.getProfesor() == null || !rutina.getProfesor().getId().equals(profesor.getId())) {
            throw new AccessDeniedException("Solo el profesor creador puede modificar o asignar esta rutina");
        }
    }

    private void cargarDias(Rutina rutina, RutinaRequest request) {
        for (var req : request.getDias()) {
            DiaRutina dia = DiaRutina.builder().nombre(req.getNombre()).numeroOrden(req.getNumeroOrden()).rutina(rutina).build();
            if (req.getEjercicios() != null) for (var e : req.getEjercicios()) {
                Ejercicio ejercicio = ejercicioService.findEntityById(e.getEjercicioId());
                dia.getEjercicios().add(DiaEjercicio.builder().diaRutina(dia).ejercicio(ejercicio)

                        .bloqueOrden(e.getBloqueOrden()).bloqueNombre(e.getBloqueNombre()).vueltas(e.getVueltas())
                        .ordenEnBloque(e.getOrdenEnBloque()).descansoEjercicioSegundos(e.getDescansoEjercicioSegundos())
                        .descansoVueltaSegundos(e.getDescansoVueltaSegundos())
                        .seriesSugeridas(e.getSeriesSugeridas()).repsSugeridas(e.getRepsSugeridas()).pesoSugerido(e.getPesoSugerido())
                        .ejercicioNombreSnapshot(ejercicio.getNombre()).ejercicioDescripcionSnapshot(ejercicio.getDescripcion())
                        .grupoMuscularSnapshot(ejercicio.getGrupoNombre()).build());
            }
            rutina.getDias().add(dia);
        }
    }

    private void copiarDias(Rutina anterior, Rutina nueva) {
        for (DiaRutina viejo : anterior.getDias()) {
            DiaRutina dia = DiaRutina.builder().nombre(viejo.getNombre()).numeroOrden(viejo.getNumeroOrden()).rutina(nueva).build();
            for (DiaEjercicio e : viejo.getEjercicios()) {
                dia.getEjercicios().add(DiaEjercicio.builder().diaRutina(dia).ejercicio(e.getEjercicio())

                        .bloqueOrden(e.getBloqueOrden()).bloqueNombre(e.getBloqueNombre()).vueltas(e.getVueltas())
                        .ordenEnBloque(e.getOrdenEnBloque()).descansoEjercicioSegundos(e.getDescansoEjercicioSegundos())
                        .descansoVueltaSegundos(e.getDescansoVueltaSegundos())
                        .seriesSugeridas(e.getSeriesSugeridas()).repsSugeridas(e.getRepsSugeridas()).pesoSugerido(e.getPesoSugerido())
                        .ejercicioNombreSnapshot(e.getNombreHistorico()).grupoMuscularSnapshot(e.getGrupoHistorico())
                        .ejercicioDescripcionSnapshot(e.getEjercicioDescripcionSnapshot()).build());
            }
            nueva.getDias().add(dia);
        }
    }

    private List<?> firma(Rutina rutina) {
        return rutina.getDias().stream().map(d -> java.util.Arrays.asList(d.getNombre(), d.getNumeroOrden(),
                d.getEjercicios().stream().map(e -> java.util.Arrays.asList(e.getEjercicio().getId(), e.getSeriesSugeridas(),
                        e.getRepsSugeridas(), e.getPesoSugerido(), e.getNombreHistorico(), e.getGrupoHistorico(),
                        e.getEjercicioDescripcionSnapshot(), e.getBloqueOrden(), e.getBloqueNombre(),e.getVueltas(), e.getOrdenEnBloque(), e.getDescansoEjercicioSegundos(), e.getDescansoVueltaSegundos())).toList())).toList();
    }

    @Transactional
    public AsignacionResponse asignarRutina(Long rutinaId, Long alumnoId) {
        return asignarRutinaMe(alumnoId, rutinaId, actual());
    }

    @Transactional
    public List<RutinaResponse> getHistorialDeAlumno(Long alumnoId, Authentication auth) {
        acceso.leerAlumno(alumnoId, auth);
        Alumno alumno = alumnoService.findEntityById(alumnoId);
        return asignacionRutinaRepository.findByAlumno(alumno).stream().map(a -> historial(a, auth)).toList();
    }

    @Transactional
    public RutinaResponse getRutinaActivaDeAlumno(Long alumnoId, Authentication auth) {
        acceso.leerAlumno(alumnoId, auth);
        Alumno alumno = alumnoService.findEntityById(alumnoId);
        AsignacionRutina asignacion = asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno)
                .orElseThrow(() -> new RecursoNoEncontradoException("El alumno no tiene una rutina activa"));
        return historial(asignacion, auth);
    }

    @Transactional
    public AsignacionResponse asignarRutinaMe(Long alumnoId, Long rutinaId, Authentication auth) {
        return asignarRutinaMe(alumnoId,rutinaId,null,auth);
    }
    @Transactional
    public AsignacionResponse asignarRutinaMe(Long alumnoId, Long rutinaId, utn.simulacro_nombreAlumno.model.request.PesosAsignacionRequest pesos, Authentication auth) {
        Profesor profesor = acceso.profesorPropio(auth);
        Alumno alumno = alumnoService.gestionarEntity(alumnoId, auth);
        Rutina raiz = raizBloqueada(rutinaId);
        exigirCreador(raiz, profesor);
        if (raiz.isArchivada()) throw new ReglaNegocioException("No se puede asignar una rutina archivada");
        Rutina rutina = ultimaBloqueada(raiz);
        asignacionRutinaRepository.findByAlumnoAndActivaTrue(alumno).ifPresent(a -> {
            a.setActiva(false);
            a.setFechaFin(LocalDateTime.now());
            asignacionRutinaRepository.saveAndFlush(a);
        });
        AsignacionRutina asignacion = AsignacionRutina.builder().rutina(rutina).alumno(alumno)
                .fechaInicio(LocalDateTime.now()).activa(true).build();
        if(pesos!=null) configurarPesos(asignacion,pesos);
        else for(var dia:rutina.getDias())for(var e:dia.getEjercicios()) if(e.getPesoSugerido()!=null)asignacion.getPesosSugeridos().put(e.getId(),e.getPesoSugerido());
        return asignacionMapper.toResponse(asignacionRutinaRepository.saveAndFlush(asignacion));
    }

    @Transactional
    public RutinaResponse getAsignacion(Long id, Authentication auth) {
        var asignacion=asignacionRutinaRepository.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Asignación no encontrada"));
        acceso.leerAlumno(asignacion.getAlumno().getId(),auth);
        return historial(asignacion,auth);
    }
    @Transactional
    public RutinaResponse editarPesos(Long id,utn.simulacro_nombreAlumno.model.request.PesosAsignacionRequest request,Authentication auth) {
        var profesor=acceso.profesorPropio(auth);
        var asignacion=asignacionRutinaRepository.bloquear(id).orElseThrow(()->new RecursoNoEncontradoException("Asignación no encontrada"));
        alumnoService.gestionarEntity(asignacion.getAlumno().getId(),auth);
        exigirCreador(asignacion.getRutina(),profesor);
        if(!asignacion.isActiva())throw new utn.simulacro_nombreAlumno.exception.ConflictoEntrenamientoException("La asignación ya no está activa");
        if(request==null || request.revision()==null || request.revision()!=asignacion.getRevision())throw new utn.simulacro_nombreAlumno.exception.ConflictoEntrenamientoException("La asignación cambió; recargá los datos");
        configurarPesos(asignacion,request);asignacionRutinaRepository.saveAndFlush(asignacion);
        return historial(asignacion,auth);
    }
    private void configurarPesos(AsignacionRutina asignacion,utn.simulacro_nombreAlumno.model.request.PesosAsignacionRequest request) {
        // Validación también para llamadas internas, no solamente desde HTTP.
        validacion.validar(request);
        var permitidos=asignacion.getRutina().getDias().stream().flatMap(d->d.getEjercicios().stream()).map(e->e.getId()).collect(java.util.stream.Collectors.toSet());
        var nuevos=new java.util.LinkedHashMap<Long,Double>();var vistos=new java.util.HashSet<Long>();
        for(var peso:request.pesos()) {
            if(!permitidos.contains(peso.diaEjercicioId()) || !vistos.add(peso.diaEjercicioId()))throw new ReglaNegocioException("Seleccioná ejercicios de la versión asignada, sin repetir posiciones");
            if(peso.pesoSugerido()!=null)nuevos.put(peso.diaEjercicioId(),peso.pesoSugerido());
        }
        asignacion.getPesosSugeridos().clear();asignacion.getPesosSugeridos().putAll(nuevos);
    }

    @Transactional
    public RutinaResponse getRutinaActivaMe(Authentication auth) {
        return getRutinaActivaDeAlumno(acceso.alumnoPropio(auth).getId(), auth);
    }

    @Transactional
    public List<RutinaResponse> getHistorialMe(Authentication auth) {
        return getHistorialDeAlumno(acceso.alumnoPropio(auth).getId(), auth);
    }

    private Authentication actual() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private RutinaResponse respuesta(Rutina rutina, Authentication auth) {
        RutinaResponse response = rutinaMapper.toDto(rutina);
        filtrarAlumnos(response, rutina, auth);
        return response;
    }

    private RutinaResponse historial(AsignacionRutina asignacion, Authentication auth) {
        RutinaResponse response = rutinaMapper.toHistorialDto(asignacion);
        filtrarAlumnos(response, asignacion.getRutina(), auth);
        return response;
    }

    private void filtrarAlumnos(RutinaResponse response, Rutina rutina, Authentication auth) {
        // El catálogo sigue compartido, pero sus asignaciones no revelan alumnos ajenos.
        response.setAlumnos(asignacionRutinaRepository.findActivasDeFamilia(rutina.getIdRaiz()).stream()
                .filter(a -> acceso.puedeLeerAlumno(a.getAlumno().getId(), auth))
                .map(a -> a.getAlumno().getNombre() + " " + a.getAlumno().getApellido()).toList());
    }
}
