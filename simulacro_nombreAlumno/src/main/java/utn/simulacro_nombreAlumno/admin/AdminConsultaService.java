package utn.simulacro_nombreAlumno.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.simulacro_nombreAlumno.exception.*;
import utn.simulacro_nombreAlumno.mapper.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.response.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.security.AccesoService;
import utn.simulacro_nombreAlumno.service.*;
import utn.simulacro_nombreAlumno.validation.ValidacionEntrenamiento;
import java.util.*;
import static utn.simulacro_nombreAlumno.admin.AdminDtos.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class AdminConsultaService {
    private final AccesoService acceso;
    private final ValidacionEntrenamiento validacion;
    private final UsuarioRepository usuarios;
    private final AlumnoRepository alumnos;
    private final ProfesorRepository profesores;
    private final RutinaRepository rutinas;
    private final AsignacionRutinaRepository asignaciones;
    private final SesionRepository sesiones;
    private final AuditoriaAdministrativaRepository auditorias;
    private final AlumnoMapper alumnoMapper;
    private final ProfesorMapper profesorMapper;
    private final AsignacionMapper asignacionMapper;
    private final SesionMapper sesionMapper;
    private final RutinaService rutinaService;
    private final SesionService sesionService;
    @PersistenceContext private EntityManager em;

    void exigir(Authentication auth) { acceso.exigirRol(auth,Rol.ADMIN); }
    public static UsuarioVista usuarioVista(Usuario u) {
        return u==null ? null : new UsuarioVista(u.getId(),u.getEmail(),u.getRol(),u.isActivo(),u.getTokenVersion(),
                u.getAlumno()==null?null:u.getAlumno().getId(),u.getProfesor()==null?null:u.getProfesor().getId());
    }
    public AlumnoVista alumnoVista(Alumno a) {
        return new AlumnoVista(alumnoMapper.toDto(a),usuarioVista(a.getUsuario()),a.getUsuario()!=null && a.getUsuario().getRol()==Rol.ALUMNO);
    }
    public ProfesorVista profesorVista(Profesor p) {
        return new ProfesorVista(profesorMapper.toDto(p),usuarioVista(p.getUsuario()),p.getUsuario()!=null && p.getUsuario().getRol()==Rol.PROFESOR);
    }
    Pageable pagina(AdminFiltro f, String... permitidos) {
        validacion.validar(f);
        if (f.getDesde()!=null && f.getHasta()!=null && f.getDesde().isAfter(f.getHasta())) throw new ReglaNegocioException("Rango de fechas inválido");
        if (!Set.of(permitidos).contains(f.getOrden())) throw new ReglaNegocioException("Orden no permitido");
        Sort sort=Sort.by(Sort.Direction.fromString(f.getDireccion()),f.getOrden());
        if (!f.getOrden().equals("id")) sort=sort.and(Sort.by("id"));
        return PageRequest.of(f.getPagina(),f.getTamanio(),sort);
    }
    private Predicate contiene(CriteriaBuilder b, Expression<String> field, String q) {
        String pattern="%"+q.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        return b.like(b.lower(field),pattern,'\\');
    }
    private boolean busca(AdminFiltro f) { return f.getQ()!=null && !f.getQ().isBlank(); }
    @SafeVarargs
    private final Predicate texto(CriteriaBuilder b, AdminFiltro f, Expression<String>... fields) {
        return b.or(Arrays.stream(fields).map(x->contiene(b,x,f.getQ())).toArray(Predicate[]::new));
    }
    public Pagina<UsuarioVista> usuarios(AdminFiltro f, Authentication auth) {
        exigir(auth); Pageable page=pagina(f,"id","email","rol","activo");
        Specification<Usuario> spec=(r,q,b)-> {
            List<Predicate> p=new ArrayList<>();
            if (f.getRol()!=null) p.add(b.equal(r.get("rol"),f.getRol()));
            if (f.getActivo()!=null) p.add(b.equal(r.get("activo"),f.getActivo()));
            if (busca(f)) {
                var a=r.join("alumno",JoinType.LEFT); var t=r.join("profesor",JoinType.LEFT);
                p.add(texto(b,f,r.get("email"),a.get("nombre"),a.get("apellido"),t.get("nombre"),t.get("apellido")));
            }
            return b.and(p.toArray(Predicate[]::new));
        };
        return Pagina.de(usuarios.findAll(spec,page).map(AdminConsultaService::usuarioVista));
    }
    public UsuarioVista usuario(Long id, Authentication auth) { exigir(auth); return usuarioVista(usuarioEntity(id)); }
    Usuario usuarioEntity(Long id) { return usuarios.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Usuario no encontrado")); }
    private void cuenta(List<Predicate> p, From<?,?> r, CriteriaBuilder b, AdminFiltro f, Rol rol) {
        var u=r.join("usuario",JoinType.LEFT);
        if(f.getSinCuenta()!=null) p.add(f.getSinCuenta()?b.isNull(u.get("id")):b.isNotNull(u.get("id")));
        if(f.getActivo()!=null) p.add(b.equal(u.get("activo"),f.getActivo()));
        if(f.getVigente()!=null) {
            Predicate vigente=b.equal(u.get("rol"),rol);
            p.add(f.getVigente()?vigente:b.or(b.isNull(u.get("id")),b.not(vigente)));
        }
    }
    public Pagina<AlumnoVista> alumnos(AdminFiltro f, Authentication auth) {
        exigir(auth); Pageable page=pagina(f,"id","nombre","apellido","email","nivel","objetivo");
        Specification<Alumno> spec=(r,q,b)-> {
            List<Predicate> p=new ArrayList<>(); cuenta(p,r,b,f,Rol.ALUMNO);
            if(busca(f)) p.add(texto(b,f,r.get("nombre"),r.get("apellido"),r.get("email")));
            if(f.getNivel()!=null) p.add(b.equal(r.get("nivel"),f.getNivel()));
            if(f.getObjetivo()!=null) p.add(b.equal(r.get("objetivo"),f.getObjetivo()));
            if(f.getProfesorId()!=null) {p.add(b.equal(r.join("profesores").get("id"),f.getProfesorId()));q.distinct(true);}
            return b.and(p.toArray(Predicate[]::new));
        };
        return Pagina.de(alumnos.findAll(spec,page).map(this::alumnoVista));
    }
    public AlumnoVista alumno(Long id, Authentication auth) { exigir(auth);return alumnoVista(alumnos.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Alumno no encontrado"))); }
    public Pagina<ProfesorVista> profesores(AdminFiltro f, Authentication auth) {
        exigir(auth); Pageable page=pagina(f,"id","nombre","apellido","email","especialidad");
        Specification<Profesor> spec=(r,q,b)-> {
            List<Predicate> p=new ArrayList<>();cuenta(p,r,b,f,Rol.PROFESOR);
            if(busca(f)) p.add(texto(b,f,r.get("nombre"),r.get("apellido"),r.get("email")));
            if(f.getEspecialidad()!=null) p.add(contiene(b,r.get("especialidad"),f.getEspecialidad()));
            if(f.getAlumnoId()!=null) {p.add(b.equal(r.join("alumnos").get("id"),f.getAlumnoId()));q.distinct(true);}
            return b.and(p.toArray(Predicate[]::new));
        };
        return Pagina.de(profesores.findAll(spec,page).map(this::profesorVista));
    }
    public ProfesorVista profesor(Long id, Authentication auth) { exigir(auth);return profesorVista(profesores.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Profesor no encontrado"))); }
    private Predicate familia(From<?,?> r, CriteriaBuilder b, Long id) {
        var raiz=r.join("raiz",JoinType.LEFT);
        return b.equal(b.coalesce(raiz.<Long>get("id"),r.<Long>get("id")),id);
    }
    public Pagina<RutinaResponse> rutinas(AdminFiltro f, Authentication auth) {
        exigir(auth); Pageable page=pagina(f,"id","nombre","capturadaEn");
        Specification<Rutina> spec=(r,q,b)-> {
            List<Predicate> p=new ArrayList<>();
            var raiz=r.join("raiz",JoinType.LEFT);
            var sub=q.subquery(Integer.class);var other=sub.from(Rutina.class);var oroot=other.join("raiz",JoinType.LEFT);
            sub.select(b.max(other.get("numeroVersion"))).where(b.equal(b.coalesce(oroot.<Long>get("id"),other.<Long>get("id")),b.coalesce(raiz.<Long>get("id"),r.<Long>get("id"))));
            p.add(b.equal(r.get("numeroVersion"),sub));
            if(busca(f)) p.add(texto(b,f,r.get("nombre"),r.get("descripcion")));
            if(f.getProfesorId()!=null) p.add(b.equal(r.get("profesor").get("id"),f.getProfesorId()));
            if(f.getArchivada()!=null) p.add(b.equal(b.coalesce(raiz.<Boolean>get("archivada"),r.<Boolean>get("archivada")),f.getArchivada()));
            return b.and(p.toArray(Predicate[]::new));
        };
        return Pagina.de(rutinas.findAll(spec,page).map(r->rutinaService.getVersion(r.getIdRaiz(),r.getNumeroVersion(),auth)));
    }
    public RutinaResponse rutina(Long id, Authentication auth) { exigir(auth);return rutinaService.findById(id); }
    public Pagina<RutinaResponse> versiones(Long id, AdminFiltro f, Authentication auth) {
        exigir(auth); Rutina root=rutinas.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Rutina no encontrada"));
        Long rootId=root.getIdRaiz();
        return Pagina.de(rutinas.findAll((r,q,b)->familia(r,b,rootId),pagina(f,"id","numeroVersion")).map(r->rutinaService.getVersion(rootId,r.getNumeroVersion(),auth)));
    }
    public Pagina<AsignacionResponse> asignaciones(AdminFiltro f, Authentication auth) {
        exigir(auth);Pageable page=pagina(f,"id","fechaInicio","fechaFin");
        Specification<AsignacionRutina> spec=(r,q,b)-> {
            List<Predicate> p=new ArrayList<>();
            if(f.getAlumnoId()!=null) p.add(b.equal(r.get("alumno").get("id"),f.getAlumnoId()));
            if(f.getProfesorId()!=null) p.add(b.equal(r.get("rutina").get("profesor").get("id"),f.getProfesorId()));
            if(f.getRutinaId()!=null) p.add(familia(r.join("rutina"),b,f.getRutinaId()));
            if(f.getActivo()!=null) p.add(b.equal(r.get("activa"),f.getActivo()));
            return b.and(p.toArray(Predicate[]::new));
        };
        return Pagina.de(asignaciones.findAll(spec,page).map(asignacionMapper::toResponse));
    }
    public AsignacionResponse asignacion(Long id, Authentication auth) { exigir(auth);return asignacionMapper.toResponse(asignaciones.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Asignación no encontrada"))); }
    public Pagina<SesionResponse> sesiones(AdminFiltro f, Authentication auth) {
        exigir(auth); Pageable page=pagina(f,"id","fecha");
        Specification<Sesion> spec=(r,q,b)-> {
            List<Predicate> p=new ArrayList<>();
            if(f.getAlumnoId()!=null) p.add(b.equal(r.get("alumno").get("id"),f.getAlumnoId()));
            if(f.getProfesorId()!=null) p.add(b.equal(r.get("diaRutina").get("rutina").get("profesor").get("id"),f.getProfesorId()));
            if(f.getRutinaId()!=null) p.add(familia(r.join("diaRutina").join("rutina"),b,f.getRutinaId()));
            if(f.getFinalizada()!=null) p.add(b.equal(r.get("completada"),f.getFinalizada()));
            Expression<java.time.LocalDate> fecha=b.coalesce(r.get("fechaCorregida"),r.get("fecha"));
            if(f.getDesde()!=null) p.add(b.greaterThanOrEqualTo(fecha,f.getDesde()));
            if(f.getHasta()!=null) p.add(b.lessThanOrEqualTo(fecha,f.getHasta()));
            return b.and(p.toArray(Predicate[]::new));
        };
        return Pagina.de(sesiones.findAll(spec,page).map(sesionMapper::toDto));
    }
    public SesionResponse sesion(Long id, Authentication auth) { exigir(auth);return sesionService.getDetalle(id,auth); }
    public List<AuditoriaEntrenamientoResponse> auditoriaSesion(Long id,Authentication auth) {exigir(auth);return sesionService.auditoria(id,auth);}
    private long contar(String query) {return em.createQuery(query,Long.class).getSingleResult();}
    public Resumen resumen(Authentication auth) {
        exigir(auth);
        return new Resumen(usuarios.count(),contar("select count(u) from Usuario u where u.activo=true"),contar("select count(u) from Usuario u where u.activo=false"),usuarios.countByRolAndActivoTrue(Rol.ADMIN),
                contar("select count(u) from Usuario u where u.rol=utn.simulacro_nombreAlumno.model.Rol.PROFESOR"),contar("select count(u) from Usuario u where u.rol=utn.simulacro_nombreAlumno.model.Rol.ALUMNO"),
                profesores.count(),alumnos.count(),contar("select count(p) from Profesor p where p.usuario is null")+contar("select count(a) from Alumno a where a.usuario is null"),
                contar("select count(p) from Alumno a join a.profesores p"),contar("select count(r) from Rutina r where r.raiz is null"),contar("select count(r) from Rutina r where r.raiz is null and r.archivada=true"),
                rutinas.count(),asignaciones.count(),contar("select count(a) from AsignacionRutina a where a.activa=true"),sesiones.count(),contar("select count(s) from Sesion s where s.completada=true"));
    }
    public Pagina<AuditoriaVista> auditoria(Long usuarioId,AdminFiltro f,Authentication auth) {
        exigir(auth); if(usuarioId!=null) usuarioEntity(usuarioId);
        return Pagina.de(auditorias.findAll((r,q,b)->usuarioId==null?b.conjunction():b.equal(r.get("usuario").get("id"),usuarioId),pagina(f,"id","creadaEn"))
                .map(a->new AuditoriaVista(a.getId(),a.getActor().getId(),a.getUsuario()==null?null:a.getUsuario().getId(),a.getAccion(),a.getMotivo(),a.getAntes(),a.getDespues(),a.getCreadaEn())));
    }
}
