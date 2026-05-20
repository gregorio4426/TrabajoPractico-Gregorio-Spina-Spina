package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

public class AsignacionRutina {
    private Long id;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activa;

    @ManyToOne
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;

    @ManyToOne
    @JoinColumn(name = "alumno_id")
    private Alumno alumno;
}
