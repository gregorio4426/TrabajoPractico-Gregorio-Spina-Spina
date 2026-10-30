package utn.simulacro_nombreAlumno.estadisticas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter
public class EstadisticaFiltro {
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    @Schema(description="Fecha efectiva inicial inclusiva. Omitida: 29 días antes de hasta.",example="2026-10-01")
    private LocalDate desde;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    @Schema(description="Fecha efectiva final inclusiva. Omitida: fecha actual UTC.",example="2026-10-30")
    private LocalDate hasta;
    @Min(0) @Max(1000000) @Schema(description="Página desde 0; solo aplica a listados paginados",defaultValue="0")
    private int pagina=0;
    @Min(1) @Max(100) @Schema(description="Elementos por página; solo aplica a listados paginados",defaultValue="20")
    private int tamanio=20;
}
