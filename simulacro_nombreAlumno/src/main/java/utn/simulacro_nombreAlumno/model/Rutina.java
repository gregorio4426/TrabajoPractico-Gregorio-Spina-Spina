package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rutinas", uniqueConstraints = @UniqueConstraint(name = "uk_rutina_version", columnNames = {"rutina_raiz_id", "numero_version"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class    Rutina {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(nullable = false, updatable = false)
	private String nombre;
    @Column(nullable = false, updatable = false)
	private String descripcion;

    @OneToMany(mappedBy = "rutina", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("numeroOrden ASC, id ASC")
    @Builder.Default
    private List<DiaRutina> dias = new ArrayList<>();

	@ManyToOne
	@JoinColumn(name = "profesor_id", updatable = false)
	private Profesor profesor;

    @OneToMany (mappedBy = "rutina")
    @Builder.Default
    private List<AsignacionRutina> asignaciones = new ArrayList<>();

    /** La fila original es versión 1 e identidad estable de toda la familia. */
    @ManyToOne
    @JoinColumn(name = "rutina_raiz_id", updatable = false)
    private Rutina raiz;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private int numeroVersion = 1;

    @Column(nullable = false)
    private boolean archivada;

    @Column(nullable = false, updatable = false, length = 32)
    @Builder.Default
    private String origenVersion = "REGISTRADA";

    @Column(updatable = false)
    private java.time.LocalDateTime capturadaEn;

    @PrePersist
    void capturarFecha() {
        if (capturadaEn == null) capturadaEn = java.time.LocalDateTime.now();
    }

    public Long getIdRaiz() { return raiz == null ? id : raiz.getId(); }
    public boolean isArchivadaEfectiva() { return raiz == null ? archivada : raiz.isArchivada(); }
}
