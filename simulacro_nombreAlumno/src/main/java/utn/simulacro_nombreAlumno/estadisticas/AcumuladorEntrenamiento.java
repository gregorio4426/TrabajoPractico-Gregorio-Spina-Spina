package utn.simulacro_nombreAlumno.estadisticas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import static utn.simulacro_nombreAlumno.estadisticas.EstadisticaDtos.*;

/** Recibe proyecciones por ID de sesión ascendente. Producto/suma decimal exactos, incluso para pesos grandes. */
final class AcumuladorEntrenamiento {
    private long registradas,abiertas,finalizadas,entrenamientos,series,excluidas,seriesAbiertas,repeticiones,conPeso,sinPeso;
    private Integer maxReps;
    private BigDecimal maxPeso,volumen;
    private final Set<Long> alumnos=new HashSet<>();
    private final Set<LocalDate> fechas=new HashSet<>();
    private LocalDate ultima;
    private Long sesionActual;
    private boolean sesionValida;
    private RegistroEstadistico primero;
    void agregar(RegistroEstadistico r) {
        if(sesionActual==null || sesionActual!=r.sesionId()) {
            cerrarSesion();sesionActual=r.sesionId();sesionValida=false;primero=r;
            registradas++;if(r.finalizada())finalizadas++;else abiertas++;
        }
        if(r.serieId()==null)return;
        if(!r.finalizada()){seriesAbiertas++;return;}
        if(!r.serieValida()){excluidas++;return;}
        sesionValida=true;series++;repeticiones=Math.addExact(repeticiones,r.repeticiones());
        maxReps=maxReps==null?r.repeticiones():Math.max(maxReps,r.repeticiones());
        if(r.peso()==null){sinPeso++;return;}
        conPeso++;BigDecimal peso=BigDecimal.valueOf(r.peso());
        maxPeso=maxPeso==null?peso:maxPeso.max(peso);
        BigDecimal carga=peso.multiply(BigDecimal.valueOf(r.repeticiones()));
        volumen=volumen==null?carga:volumen.add(carga);
    }
    private void cerrarSesion() {
        if(sesionActual!=null&&sesionValida){
            entrenamientos++;alumnos.add(primero.alumnoId());fechas.add(primero.fechaEfectiva());
            if(ultima==null||primero.fechaEfectiva().isAfter(ultima))ultima=primero.fechaEfectiva();
        }
        sesionActual=null;
    }
    Metricas resultado() {
        cerrarSesion();
        return new Metricas(registradas,abiertas,finalizadas,entrenamientos,finalizadas-entrenamientos,alumnos.size(),fechas.size(),ultima,
                series,excluidas,seriesAbiertas,repeticiones,maxReps,conPeso,sinPeso,maxPeso,volumen,series>0&&sinPeso==0);
    }
}
