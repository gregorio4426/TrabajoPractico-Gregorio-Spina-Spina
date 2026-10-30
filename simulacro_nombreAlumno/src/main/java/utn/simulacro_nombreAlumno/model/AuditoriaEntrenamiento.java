package utn.simulacro_nombreAlumno.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Immutable
@Table(name = "auditoria_entrenamientos", indexes = @Index(name = "ix_auditoria_sesion", columnList = "sesion_id,id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditoriaEntrenamiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne @JoinColumn(name = "sesion_id", nullable = false, updatable = false) private Sesion sesion;
    @ManyToOne @JoinColumn(name = "serie_id", updatable = false) private Serie serie;
    @ManyToOne @JoinColumn(name = "actor_id", nullable = false, updatable = false) private Usuario actor;
    @Column(nullable = false, length = 32, updatable = false) private String accion;
    @Column(nullable = false, length = 500, updatable = false) private String motivo;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR)
    @Column(nullable = false, updatable = false, length = org.hibernate.Length.LONG32) private String antes;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.LONGVARCHAR)
    @Column(nullable = false, updatable = false, length = org.hibernate.Length.LONG32) private String despues;
    @Column(nullable = false, updatable = false) private LocalDateTime creadaEn;
}
