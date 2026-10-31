package utn.simulacro_nombreAlumno.estadisticas;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.admin.AdminConsultaService;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.AsignacionMapper;
import utn.simulacro_nombreAlumno.model.Rol;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.AccesoService;
import utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import static utn.simulacro_nombreAlumno.estadisticas.EstadisticaDtos.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN','PROFESOR','ALUMNO')")
public class EstadisticaService {
    /** Constante interna elegida por el controller; jamás se recibe del cliente. */
    public enum Alcance { GIMNASIO, MIS_ALUMNOS, PROPIO, ALUMNO }
    private final EstadisticaRepository repository;
    private final AccesoService acceso;
    private final AlumnoRepository alumnos;
    private final EjercicioRepository ejercicios;
    private final AsignacionRutinaRepository asignaciones;
    private final AsignacionMapper asignacionMapper;
    private final AdminConsultaService administracion;
    private final ValidacionEntrenamiento validacion;
    private final Clock clock;

    private AmbitoEstadistico ambito(Alcance alcance,Long alumnoId,Authentication auth) {
        return switch(alcance) {
            case GIMNASIO -> {acceso.exigirRol(auth,Rol.ADMIN);yield new AmbitoEstadistico(null,null);}
            case MIS_ALUMNOS -> new AmbitoEstadistico(null,acceso.profesorPropio(auth).getId());
            case PROPIO -> new AmbitoEstadistico(acceso.alumnoPropio(auth).getId(),null);
            case ALUMNO -> {
                acceso.leerAlumno(alumnoId,auth);
                if(!alumnos.existsById(alumnoId))throw new RecursoNoEncontradoException("Alumno no encontrado");
                var user=acceso.usuario(auth);
                yield new AmbitoEstadistico(alumnoId,user.getRol()==Rol.PROFESOR?acceso.profesorPropio(auth).getId():null);
            }
        };
    }
    private Periodo periodo(EstadisticaFiltro filtro){validacion.validar(filtro);return PeriodoEstadistico.resolver(filtro,clock);}
    private Metricas medir(AmbitoEstadistico a,Periodo p) {
        var acc=new AcumuladorEntrenamiento();repository.recorrer(a,p,false,null,null,null,null,acc::agregar);return acc.resultado();
    }
    public Resumen resumen(Alcance tipo,Long alumnoId,EstadisticaFiltro filtro,Authentication auth) {
        var a=ambito(tipo,alumnoId,auth);var p=periodo(filtro);var m=medir(a,p);return new Resumen(p,a.alumnoId(),m,FrecuenciaEstadistica.calcular(m,p));
    }
    private <T> PaginaEstadistica<T> pagina(Periodo p,EstadisticaFiltro f,long total,List<T> items) {
        return new PaginaEstadistica<>(p,items,f.getPagina(),f.getTamanio(),total,total/f.getTamanio()+(total%f.getTamanio()==0?0:1));
    }
    public PaginaEstadistica<ActividadDia> actividad(Alcance tipo,Long alumnoId,EstadisticaFiltro f,Authentication auth) {
        var a=ambito(tipo,alumnoId,auth);var p=periodo(f);long total=repository.contarDias(a,p);var claves=repository.dias(a,p,f);
        Map<LocalDate,AcumuladorEntrenamiento> acc=new LinkedHashMap<>();claves.forEach(k->acc.put(k,new AcumuladorEntrenamiento()));
        if(!claves.isEmpty())repository.recorrer(a,p,false,claves,null,null,null,r->acc.get(r.fechaEfectiva()).agregar(r));
        return pagina(p,f,total,acc.entrySet().stream().map(e->new ActividadDia(e.getKey(),e.getValue().resultado())).toList());
    }
    public PaginaEstadistica<EjercicioEstadistica> ejercicios(Alcance tipo,Long alumnoId,EstadisticaFiltro f,Authentication auth) {
        var a=ambito(tipo,alumnoId,auth);var p=periodo(f);long total=repository.contarEjercicios(a,p);var claves=repository.ejercicios(a,p,f);
        Map<Long,AcumuladorEntrenamiento> acc=new HashMap<>();claves.forEach(k->acc.put(k.id(),new AcumuladorEntrenamiento()));
        if(!claves.isEmpty())repository.recorrer(a,p,true,null,claves.stream().map(EstadisticaRepository.EjercicioClave::id).toList(),null,null,r->acc.get(r.ejercicioId()).agregar(r));
        return pagina(p,f,total,claves.stream().map(k->new EjercicioEstadistica(k.id(),k.nombre(),acc.get(k.id()).resultado())).toList());
    }
    public PaginaEstadistica<EvolucionEjercicio> evolucion(Alcance tipo,Long alumnoId,Long ejercicioId,EstadisticaFiltro f,Authentication auth) {
        var a=ambito(tipo,alumnoId,auth);var p=periodo(f);
        if(!ejercicios.existsById(ejercicioId))throw new RecursoNoEncontradoException("Ejercicio no encontrado");
        long total=repository.contarEvolucion(a,p,ejercicioId);var claves=repository.sesionesEjercicio(a,p,ejercicioId,f);
        Map<Long,AcumuladorEntrenamiento> acc=new HashMap<>();Map<Long,RegistroEstadistico> primero=new HashMap<>();
        claves.forEach(k->acc.put(k,new AcumuladorEntrenamiento()));
        if(!claves.isEmpty())repository.recorrer(a,p,true,null,List.of(ejercicioId),claves,null,r->{primero.putIfAbsent(r.sesionId(),r);acc.get(r.sesionId()).agregar(r);});
        return pagina(p,f,total,claves.stream().map(k->{var r=primero.get(k);return new EvolucionEjercicio(ejercicioId,r.nombreHistorico(),k,r.alumnoId(),r.fechaOriginal(),r.fechaEfectiva(),acc.get(k).resultado());}).toList());
    }
    @PreAuthorize("hasRole('PROFESOR')")
    public PaginaEstadistica<AlumnoEstadistica> alumnosProfesor(EstadisticaFiltro f,Authentication auth) {
        var a=ambito(Alcance.MIS_ALUMNOS,null,auth);var p=periodo(f);long total=repository.contarAlumnos(a.profesorId());var claves=repository.alumnos(a.profesorId(),f);
        Map<Long,AcumuladorEntrenamiento> acc=new HashMap<>();claves.forEach(k->acc.put(k.id(),new AcumuladorEntrenamiento()));
        if(!claves.isEmpty())repository.recorrer(a,p,false,null,null,null,claves.stream().map(EstadisticaRepository.AlumnoClave::id).toList(),r->acc.get(r.alumnoId()).agregar(r));
        return pagina(p,f,total,claves.stream().map(k->{var m=acc.get(k.id()).resultado();return new AlumnoEstadistica(k.id(),k.nombre(),k.apellido(),m,FrecuenciaEstadistica.calcular(m,p));}).toList());
    }
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardAdmin dashboardAdmin(EstadisticaFiltro f,Authentication auth) {
        var a=ambito(Alcance.GIMNASIO,null,auth);var p=periodo(f);var m=medir(a,p);return new DashboardAdmin(p,administracion.resumen(auth),m,FrecuenciaEstadistica.calcular(m,p));
    }
    @PreAuthorize("hasRole('PROFESOR')")
    public DashboardProfesor dashboardProfesor(EstadisticaFiltro f,Authentication auth) {
        var a=ambito(Alcance.MIS_ALUMNOS,null,auth);var p=periodo(f);long id=a.profesorId();
        var m=medir(a,p);return new DashboardProfesor(p,id,repository.contarAlumnos(id),repository.rutinasProfesor(id,false),repository.rutinasProfesor(id,true),repository.asignacionesProfesor(id),m,FrecuenciaEstadistica.calcular(m,p));
    }
    @PreAuthorize("hasRole('ALUMNO')")
    public DashboardAlumno dashboardAlumno(EstadisticaFiltro f,Authentication auth) {
        var a=ambito(Alcance.PROPIO,null,auth);var p=periodo(f);
        var alumno=alumnos.findById(a.alumnoId()).orElseThrow(()->new RecursoNoEncontradoException("Alumno no encontrado"));
        var activa=asignaciones.findByAlumnoAndActivaTrue(alumno).map(asignacionMapper::toResponse).orElse(null);
        var m=medir(a,p);return new DashboardAlumno(p,a.alumnoId(),activa,m,FrecuenciaEstadistica.calcular(m,p));
    }
}
