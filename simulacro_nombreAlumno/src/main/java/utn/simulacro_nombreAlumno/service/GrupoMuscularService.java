package utn.simulacro_nombreAlumno.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.repository.*;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class GrupoMuscularService {
 private final GrupoMuscularRepository grupos;
 private final EjercicioRepository ejercicios;
 public record Vista(Long id,String nombre) {}
 public static String normalizar(String nombre) {
  return java.text.Normalizer.normalize(nombre.strip(),java.text.Normalizer.Form.NFD)
   .replaceAll("\\p{M}","").replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
 }
 public GrupoMuscularCatalogo buscar(Long id) { return grupos.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Grupo muscular no encontrado")); }
 public List<Vista> listar() {
  // Inicialización compatible también con perfiles H2 de prueba; MySQL se inicializa mediante migración.
  for(var base:GrupoMuscular.values()) if(grupos.findByNormalizado(normalizar(base.name())).isEmpty()) guardar(base.name());
  return grupos.findAll().stream().sorted(Comparator.comparing(GrupoMuscularCatalogo::getNombre,String.CASE_INSENSITIVE_ORDER)).map(g->new Vista(g.getId(),g.getNombre())).toList();
 }
 private GrupoMuscularCatalogo guardar(String nombre) { var g=new GrupoMuscularCatalogo();g.setNombre(nombre.strip().replaceAll("\\s+"," "));g.setNormalizado(normalizar(nombre));return grupos.save(g); }
 @PreAuthorize("hasRole('PROFESOR')") public Vista crear(String nombre) { var g=grupos.findByNormalizado(normalizar(nombre)).orElseGet(()->guardar(nombre));return new Vista(g.getId(),g.getNombre()); }
 private boolean base(GrupoMuscularCatalogo g) { return Arrays.stream(GrupoMuscular.values()).anyMatch(v->normalizar(v.name()).equals(g.getNormalizado())); }
 @PreAuthorize("hasRole('PROFESOR')") public Vista editar(Long id,String nombre) {
  var g=buscar(id);if(base(g)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Los grupos estándar se conservan por compatibilidad");
  grupos.findByNormalizado(normalizar(nombre)).filter(x->!x.getId().equals(id)).ifPresent(x->{throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un grupo con ese nombre");});
  g.setNombre(nombre.strip().replaceAll("\\s+"," "));g.setNormalizado(normalizar(nombre));return new Vista(g.getId(),g.getNombre());
 }
 @PreAuthorize("hasRole('PROFESOR')") public void eliminar(Long id) {
  var g=buscar(id);if(base(g)||ejercicios.existsByGrupoPersonalizado_Id(id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"El grupo es estándar o está en uso");grupos.delete(g);
 }
}
