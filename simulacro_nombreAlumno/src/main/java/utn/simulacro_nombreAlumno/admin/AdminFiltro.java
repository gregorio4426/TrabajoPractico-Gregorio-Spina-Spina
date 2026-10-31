package utn.simulacro_nombreAlumno.admin;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import utn.simulacro_nombreAlumno.model.*;
import java.time.LocalDate;

@Getter @Setter
public class AdminFiltro {
    @Min(0) @Max(1000000) private int pagina = 0;
    @Min(1) @Max(100) private int tamanio = 20;
    @Size(max=150) private String q;
    private Rol rol;
    private Boolean activo;
    private Boolean vigente;
    private Boolean sinCuenta;
    private Nivel nivel;
    private Objetivo objetivo;
    @Size(max=100) private String especialidad;
    @Positive private Long profesorId;
    @Positive private Long alumnoId;
    @Positive private Long rutinaId;
    private Boolean archivada;
    private Boolean finalizada;
    private LocalDate desde;
    private LocalDate hasta;
    private String orden = "id";
    @jakarta.validation.constraints.Pattern(regexp="asc|desc") private String direccion = "asc";
}
