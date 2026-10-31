package utn.simulacro_nombreAlumno.model.response;

import lombok.*;
import utn.simulacro_nombreAlumno.model.Nivel;
import utn.simulacro_nombreAlumno.model.Objetivo;
import java.util.List;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlumnoResponse {

    private String nombre;
    private String apellido;
    private String email;
    private Integer edad;
    private LocalDate fechaNacimiento;

    private Long id;
    private Nivel nivel;
    private Objetivo objetivo;
    private List<ProfesorResumenResponse> profesores;

    // Conserva el constructor anterior para consumidores Java existentes.
    public AlumnoResponse(String nombre, String apellido, String email, Integer edad, LocalDate fechaNacimiento) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.edad = edad;
        this.fechaNacimiento = fechaNacimiento;
    }
    private String emailContacto;
}

