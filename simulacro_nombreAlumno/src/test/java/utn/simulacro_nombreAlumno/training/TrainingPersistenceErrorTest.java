package utn.simulacro_nombreAlumno.training;

import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaSystemException;
import utn.simulacro_nombreAlumno.exception.GlobalExceptionHandler;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.*;

class TrainingPersistenceErrorTest {
    @Test void mysqlCheckViolationIsConflictWithoutLeakingSql() {
        var response = new GlobalExceptionHandler().handleJpa(new JpaSystemException(
                new RuntimeException(new SQLException("sensitive SQL", "HY000", 3819))));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getMensaje()).doesNotContain("SQL", "sensitive");
    }
    @Test void unrelatedDatabaseFailureIsNotMisrepresentedAsConflict() {
        var response = new GlobalExceptionHandler().handleJpa(new JpaSystemException(
                new RuntimeException(new SQLException("sensitive SQL", "08000", 0))));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMensaje()).doesNotContain("SQL", "sensitive");
    }
}
