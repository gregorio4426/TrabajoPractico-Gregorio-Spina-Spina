package utn.simulacro_nombreAlumno.estadisticas;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import static utn.simulacro_nombreAlumno.estadisticas.EstadisticaDtos.*;

@Repository @RequiredArgsConstructor
class EstadisticaRepository {
    private final JdbcTemplate jdbc;
    private static final String FECHA="COALESCE(s.fecha_corregida,s.fecha)";
    // Misma regla que Serie.isVigenteValida, más coherencia de identidad/día.
    private static final String VALIDA="se.id IS NOT NULL AND se.anulada=false AND se.legado_exento=false AND se.completada=true "
            +"AND se.numero_serie>0 AND se.reps_hechas>0 AND (se.peso_usado IS NULL OR (se.peso_usado>=0 AND se.peso_usado<=1.7976931348623157E308)) "
            +"AND se.ejercicio_id=de.ejercicio_id AND de.dia_rutina_id=s.dia_rutina_id";
    private static final String FROM=" FROM sesiones s LEFT JOIN series se ON se.sesion_id=s.id "
            +"LEFT JOIN dias_ejercicio de ON de.id=se.dia_ejercicio_id LEFT JOIN ejercicios e ON e.id=se.ejercicio_id WHERE ";
    private static class Consulta {
        String sql; final List<Object> valores=new ArrayList<>();
        Consulta(String sql){this.sql=sql;}
        Consulta agregar(String sql,Object...valores){this.sql+=sql;this.valores.addAll(Arrays.asList(valores));return this;}
    }
    private Consulta base(AmbitoEstadistico a,Periodo p,boolean soloValidas) {
        Consulta c=new Consulta(FROM+FECHA+">=? AND "+FECHA+"<=?");c.valores.add(p.desde());c.valores.add(p.hasta());
        if(a.alumnoId()!=null)c.agregar(" AND s.alumno_id=?",a.alumnoId());
        if(a.profesorId()!=null)c.agregar(" AND EXISTS(SELECT 1 FROM alumnos_profesore ap WHERE ap.alumno_id=s.alumno_id AND ap.profesor_id=?)",a.profesorId());
        if(soloValidas)c.agregar(" AND s.completada=true AND "+VALIDA);
        return c;
    }
    private void lista(Consulta c,String columna,Collection<?> ids) {
        if(ids==null)return;
        if(ids.isEmpty()){c.agregar(" AND 1=0");return;}
        c.sql+=" AND "+columna+" IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")";
        c.valores.addAll(ids);
    }
    private long contar(Consulta c,String clave) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT "+clave+c.sql+" GROUP BY "+clave+") claves",Long.class,c.valores.toArray()));
    }
    private void paginar(Consulta c,EstadisticaFiltro f){c.agregar(" LIMIT ? OFFSET ?",f.getTamanio(),Math.multiplyExact((long)f.getPagina(),f.getTamanio()));}
    long contarDias(AmbitoEstadistico a,Periodo p){return contar(base(a,p,false),FECHA);}
    List<LocalDate> dias(AmbitoEstadistico a,Periodo p,EstadisticaFiltro f){
        Consulta c=base(a,p,false);c.sql="SELECT "+FECHA+" AS fecha"+c.sql+" GROUP BY "+FECHA+" ORDER BY fecha ASC";paginar(c,f);
        return jdbc.query(c.sql,(rs,n)->rs.getObject("fecha",LocalDate.class),c.valores.toArray());
    }
    record EjercicioClave(long id,String nombre){}
    long contarEjercicios(AmbitoEstadistico a,Periodo p){return contar(base(a,p,true),"e.id");}
    List<EjercicioClave> ejercicios(AmbitoEstadistico a,Periodo p,EstadisticaFiltro f){
        Consulta c=base(a,p,true);c.sql="SELECT e.id,e.nombre"+c.sql+" GROUP BY e.id,e.nombre ORDER BY e.id ASC";paginar(c,f);
        return jdbc.query(c.sql,(rs,n)->new EjercicioClave(rs.getLong(1),rs.getString(2)),c.valores.toArray());
    }
    long contarEvolucion(AmbitoEstadistico a,Periodo p,long ejercicioId){return contar(base(a,p,true).agregar(" AND se.ejercicio_id=?",ejercicioId),"s.id");}
    List<Long> sesionesEjercicio(AmbitoEstadistico a,Periodo p,long ejercicioId,EstadisticaFiltro f){
        Consulta c=base(a,p,true).agregar(" AND se.ejercicio_id=?",ejercicioId);
        c.sql="SELECT s.id,"+FECHA+" AS fecha"+c.sql+" GROUP BY s.id,"+FECHA+" ORDER BY fecha ASC,s.id ASC";paginar(c,f);
        return jdbc.query(c.sql,(rs,n)->rs.getLong(1),c.valores.toArray());
    }
    record AlumnoClave(long id,String nombre,String apellido){}
    private Consulta alumnosBase(long profesorId){return new Consulta(" FROM alumnos a WHERE EXISTS(SELECT 1 FROM alumnos_profesore ap WHERE ap.alumno_id=a.id AND ap.profesor_id=?)").agregar("",profesorId);}
    long contarAlumnos(long profesorId){var c=alumnosBase(profesorId);return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*)"+c.sql,Long.class,c.valores.toArray()));}
    List<AlumnoClave> alumnos(long profesorId,EstadisticaFiltro f){
        Consulta c=alumnosBase(profesorId);c.sql="SELECT a.id,a.nombre,a.apellido"+c.sql+" ORDER BY a.id ASC";paginar(c,f);
        return jdbc.query(c.sql,(rs,n)->new AlumnoClave(rs.getLong(1),rs.getString(2),rs.getString(3)),c.valores.toArray());
    }
    long rutinasProfesor(long id,boolean archivadas){return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM rutinas WHERE rutina_raiz_id IS NULL AND profesor_id=?"+(archivadas?" AND archivada=true":""),Long.class,id));}
    long asignacionesProfesor(long id){return Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) FROM asignaciones_rutina ar WHERE ar.activa=true AND EXISTS(SELECT 1 FROM alumnos_profesore ap WHERE ap.alumno_id=ar.alumno_id AND ap.profesor_id=?)",Long.class,id));}

    /** Filtra las claves de una página en SQL y recorre proyecciones, nunca el historial entero como entidades. */
    void recorrer(AmbitoEstadistico a,Periodo p,boolean soloValidas,Collection<LocalDate> dias,Collection<Long> ejercicios,
            Collection<Long> sesiones,Collection<Long> alumnos,Consumer<RegistroEstadistico> consumer) {
        Consulta c=base(a,p,soloValidas);lista(c,FECHA,dias);lista(c,"se.ejercicio_id",ejercicios);lista(c,"s.id",sesiones);lista(c,"s.alumno_id",alumnos);
        c.sql="SELECT s.id AS sesion_id,s.alumno_id,s.fecha,"+FECHA+" AS fecha_efectiva,s.completada AS finalizada,"
                +"se.id AS serie_id,se.ejercicio_id,de.ejercicio_nombre_snapshot,se.numero_serie,se.reps_hechas,se.peso_usado,"
                +"se.completada,se.anulada,se.legado_exento,CASE WHEN se.ejercicio_id=de.ejercicio_id AND de.dia_rutina_id=s.dia_rutina_id THEN true ELSE false END AS coherente"
                +c.sql+" ORDER BY s.id ASC,se.id ASC";
        jdbc.query(connection->{
            var ps=connection.prepareStatement(c.sql,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            try {
                for(int i=0;i<c.valores.size();i++)ps.setObject(i+1,c.valores.get(i));
                // Streaming real del driver MySQL, sin cambiar propiedades globales ni materializar filas.
                ps.setFetchSize(connection.getMetaData().getDatabaseProductName().equals("MySQL")?Integer.MIN_VALUE:100);
                return ps;
            }catch(SQLException|RuntimeException ex){ps.close();throw ex;}
        },(RowCallbackHandler)rs->consumer.accept(new RegistroEstadistico(rs.getLong("sesion_id"),rs.getLong("alumno_id"),
                rs.getObject("fecha",LocalDate.class),rs.getObject("fecha_efectiva",LocalDate.class),rs.getBoolean("finalizada"),
                rs.getObject("serie_id",Long.class),rs.getObject("ejercicio_id",Long.class),rs.getString("ejercicio_nombre_snapshot"),
                rs.getObject("numero_serie",Integer.class),rs.getObject("reps_hechas",Integer.class),rs.getObject("peso_usado",Double.class),
                rs.getBoolean("completada"),rs.getBoolean("anulada"),rs.getBoolean("legado_exento"),rs.getBoolean("coherente"))));
    }
}
