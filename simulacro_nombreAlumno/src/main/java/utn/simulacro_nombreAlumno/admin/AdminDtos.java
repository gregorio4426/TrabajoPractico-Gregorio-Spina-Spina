package utn.simulacro_nombreAlumno.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import utn.simulacro_nombreAlumno.model.*;
import utn.simulacro_nombreAlumno.model.response.*;
import java.time.*;
import java.util.List;

/** Contratos administrativos: nunca serializar entidades de autenticación. */
public final class AdminDtos {
    private AdminDtos() {}
    public record AlumnoDatos(@NotBlank @Size(max=20) String nombre, @NotBlank @Size(max=20) String apellido,
            @NotNull @Min(1) @Max(150) Integer edad, @Past LocalDate fechaNacimiento,
            @NotNull Nivel nivel, @NotNull Objetivo objetivo, @Email @Size(max=150) String emailContacto) {}
    public record ProfesorDatos(@NotBlank @Size(max=20) String nombre, @NotBlank @Size(max=20) String apellido,
            @NotBlank @Size(max=100) String especialidad, @Email @Size(max=150) String emailContacto) {}
    public record CrearUsuario(@NotBlank @Email @Size(max=150) String email, @NotBlank String password,
            @NotNull Rol rol, @Valid AlumnoDatos alumno, @Valid ProfesorDatos profesor) {}
    public record CrearAlumno(@NotBlank @Email @Size(max=150) String email, @NotBlank String password,
            @NotNull @Valid AlumnoDatos perfil) {}
    public record CrearProfesor(@NotBlank @Email @Size(max=150) String email, @NotBlank String password,
            @NotNull @Valid ProfesorDatos perfil) {}
    public record CrearCuentaPerfil(@NotBlank String password, @NotBlank @Size(max=500) String motivo) {}
    public record CambioRol(@NotNull Rol rol, @NotNull @PositiveOrZero Long revision,
            @NotBlank @Size(max=500) String motivo, @Valid AlumnoDatos alumno, @Valid ProfesorDatos profesor) {}
    public record CambioEstado(@NotNull Boolean activo, @NotNull @PositiveOrZero Long revision,
            @NotBlank @Size(max=500) String motivo) {}
    public record CambioEmail(@NotBlank @Email @Size(max=150) String email, @NotNull @PositiveOrZero Long revision,
            @NotBlank @Size(max=500) String motivo) {}
    public record CambioVinculo(@NotBlank @Size(max=500) String motivo) {}
    public record ReemplazoVinculo(@NotNull @Positive Long profesorAnteriorId, @NotNull @Positive Long profesorNuevoId,
            @NotBlank @Size(max=500) String motivo) {}
    public record UsuarioVista(Long id, String email, Rol rol, boolean activo, long revision,
            Long alumnoId, Long profesorId) {}
    public record AlumnoVista(AlumnoResponse perfil, UsuarioVista cuenta, boolean perfilVigente) {}
    public record ProfesorVista(ProfesorResponse perfil, UsuarioVista cuenta, boolean perfilVigente) {}
    public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {
        public static <T> Pagina<T> de(org.springframework.data.domain.Page<T> page) {
            return new Pagina<>(page.getContent(),page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
        }
    }
    public record Resumen(long usuarios, long cuentasActivas, long cuentasInactivas, long administradoresActivos,
            long profesoresVigentes, long alumnosVigentes, long perfilesProfesores, long perfilesAlumnos,
            long perfilesSinCuenta, long vinculaciones, long rutinas, long rutinasArchivadas,
            long versionesRutina, long asignaciones, long asignacionesActivas, long sesiones, long sesionesFinalizadas) {}
    public record AuditoriaVista(Long id, Long actorId, Long usuarioId, String accion, String motivo,
            String antes, String despues, LocalDateTime creadaEn) {}
}
