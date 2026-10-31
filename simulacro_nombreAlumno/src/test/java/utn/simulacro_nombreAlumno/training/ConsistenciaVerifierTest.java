package utn.simulacro_nombreAlumno.training;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import utn.simulacro_nombreAlumno.config.ConsistenciaMigrationVerifier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ConsistenciaVerifierTest {
    @Test void requiresMigrationMarker() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(0L, 0L);
        assertThatThrownBy(() -> new ConsistenciaMigrationVerifier(jdbc).run(null)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsInconsistentBackfill() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L, 1L);
        assertThatThrownBy(() -> new ConsistenciaMigrationVerifier(jdbc).run(null)).isInstanceOf(IllegalStateException.class);
    }
    @Test void acceptsAppliedConsistentMigration() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L, 0L);
        assertThatCode(() -> new ConsistenciaMigrationVerifier(jdbc).run(null)).doesNotThrowAnyException();
    }
}
