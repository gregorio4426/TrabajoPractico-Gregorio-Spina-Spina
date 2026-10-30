package utn.simulacro_nombreAlumno.history;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import utn.simulacro_nombreAlumno.config.HistorialMigrationVerifier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HistorialMigrationVerifierTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final HistorialMigrationVerifier verifier = new HistorialMigrationVerifier(jdbc);

    @Test
    void absentMigrationMarkerPreventsStartup() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        assertThatThrownBy(() -> verifier.run(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("migración 002");
    }

    @Test
    void pendingBackfillPreventsStartupEvenWithMarker() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L, 1L, 0L, 0L);
        assertThatThrownBy(() -> verifier.run(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("incompleta");
    }

    @Test
    void mismatchedAssignmentPreventsStartup() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L, 0L, 0L, 1L);
        assertThatThrownBy(() -> verifier.run(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("inconsistentes");
    }

    @Test
    void completedMigrationCanStart() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L, 0L, 0L, 0L);
        assertThatCode(() -> verifier.run(null)).doesNotThrowAnyException();
    }
}
