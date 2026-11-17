# syntax=docker/dockerfile:1
FROM mysql:8.4
# Copy versioned migrations with readable image permissions, independently of host file modes.
COPY --chmod=644 simulacro_nombreAlumno/src/main/resources/db/mysql/*.sql /docker-entrypoint-initdb.d/
