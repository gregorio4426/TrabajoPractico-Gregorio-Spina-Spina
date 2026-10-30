-- MySQL 8.0+ / InnoDB. Aplicar con la aplicación DETENIDA y backup verificado.
-- No borra ni renumera filas. Reanudable después de un DDL parcial.
CREATE TABLE IF NOT EXISTS gimnasio_schema_migrations (
    version_num INT PRIMARY KEY,
    aplicada_en DATETIME(6) NOT NULL
) ENGINE=InnoDB;

DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s3_columna$$
CREATE PROCEDURE gimnasio_s3_columna(IN tabla VARCHAR(64), IN columna VARCHAR(64), IN definicion VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = columna) THEN
        SET @gimnasio_s3_sql = CONCAT('ALTER TABLE `', tabla, '` ADD COLUMN `', columna, '` ', definicion);
        PREPARE sentencia FROM @gimnasio_s3_sql;
        EXECUTE sentencia;
        DEALLOCATE PREPARE sentencia;
    END IF;
END$$
DROP PROCEDURE IF EXISTS gimnasio_s3_migrar$$
CREATE PROCEDURE gimnasio_s3_migrar()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF NOT EXISTS (SELECT 1 FROM gimnasio_schema_migrations WHERE version_num = 2) THEN
        -- Ante relaciones incompletas o inconsistentes se detiene, no se inventan datos.
        IF EXISTS (SELECT 1 FROM sesiones s LEFT JOIN alumnos a ON a.id = s.alumno_id LEFT JOIN dias_rutina d ON d.id = s.dia_rutina_id LEFT JOIN rutinas r ON r.id = d.rutina_id WHERE a.id IS NULL OR d.id IS NULL OR r.id IS NULL)
            OR EXISTS (SELECT 1 FROM series se LEFT JOIN sesiones s ON s.id = se.sesion_id LEFT JOIN dias_ejercicio de ON de.id = se.dia_ejercicio_id LEFT JOIN ejercicios e ON e.id = de.ejercicio_id WHERE s.id IS NULL OR de.id IS NULL OR e.id IS NULL OR de.dia_rutina_id IS NULL OR de.dia_rutina_id <> s.dia_rutina_id)
            OR EXISTS (SELECT 1 FROM dias_ejercicio de LEFT JOIN ejercicios e ON e.id = de.ejercicio_id LEFT JOIN dias_rutina d ON d.id = de.dia_rutina_id WHERE e.id IS NULL OR d.id IS NULL)
            OR EXISTS (SELECT 1 FROM dias_rutina d LEFT JOIN rutinas r ON r.id = d.rutina_id WHERE r.id IS NULL)
            OR EXISTS (SELECT 1 FROM asignaciones_rutina a LEFT JOIN alumnos al ON al.id = a.alumno_id LEFT JOIN rutinas r ON r.id = a.rutina_id WHERE al.id IS NULL OR r.id IS NULL)
        THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Historial inconsistente: revisar relaciones antes de migrar'; END IF;

        CALL gimnasio_s3_columna('rutinas', 'rutina_raiz_id', 'BIGINT NULL');
        CALL gimnasio_s3_columna('rutinas', 'numero_version', 'INT NOT NULL DEFAULT 1');
        CALL gimnasio_s3_columna('rutinas', 'archivada', 'BIT(1) NOT NULL DEFAULT b''0''');
        CALL gimnasio_s3_columna('rutinas', 'origen_version', 'VARCHAR(32) NOT NULL DEFAULT ''LEGADO_OBSERVADO''');
        CALL gimnasio_s3_columna('rutinas', 'capturada_en', 'DATETIME(6) NULL');
        CALL gimnasio_s3_columna('dias_ejercicio', 'ejercicio_nombre_snapshot', 'VARCHAR(60) NULL');
        CALL gimnasio_s3_columna('dias_ejercicio', 'ejercicio_descripcion_snapshot', 'VARCHAR(255) NULL');
        CALL gimnasio_s3_columna('dias_ejercicio', 'grupo_muscular_snapshot', 'VARCHAR(32) NULL');
        CALL gimnasio_s3_columna('sesiones', 'asignacion_id', 'BIGINT NULL');
        CALL gimnasio_s3_columna('sesiones', 'estado_vinculo_asignacion', 'VARCHAR(40) NOT NULL DEFAULT ''LEGADO_SIN_EVALUAR''');
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'rutinas' AND constraint_name = 'fk_rutina_raiz') THEN
            ALTER TABLE rutinas ADD CONSTRAINT fk_rutina_raiz FOREIGN KEY (rutina_raiz_id) REFERENCES rutinas(id);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'rutinas' AND index_name = 'uk_rutina_version') THEN
            ALTER TABLE rutinas ADD CONSTRAINT uk_rutina_version UNIQUE (rutina_raiz_id, numero_version);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'sesiones' AND constraint_name = 'fk_sesion_asignacion') THEN
            ALTER TABLE sesiones ADD CONSTRAINT fk_sesion_asignacion FOREIGN KEY (asignacion_id) REFERENCES asignaciones_rutina(id);
        END IF;
        START TRANSACTION;
        UPDATE rutinas SET capturada_en = CURRENT_TIMESTAMP(6) WHERE capturada_en IS NULL;
        UPDATE dias_ejercicio de JOIN ejercicios e ON e.id = de.ejercicio_id
            SET de.ejercicio_nombre_snapshot = COALESCE(de.ejercicio_nombre_snapshot, e.nombre),
                de.ejercicio_descripcion_snapshot = COALESCE(de.ejercicio_descripcion_snapshot, e.descripcion),
                de.grupo_muscular_snapshot = COALESCE(de.grupo_muscular_snapshot, CAST(e.grupo_muscular AS CHAR));
        DROP TEMPORARY TABLE IF EXISTS gimnasio_s3_candidatas;
        CREATE TEMPORARY TABLE gimnasio_s3_candidatas AS
            SELECT s.id AS sesion_id, COUNT(a.id) AS candidatas, MIN(a.id) AS asignacion_id,
                MIN(CASE WHEN a.fecha_inicio <= TIMESTAMP(s.fecha) AND (a.fecha_fin IS NULL
                    OR a.fecha_fin >= TIMESTAMP(DATE_ADD(s.fecha, INTERVAL 1 DAY))) THEN 1 ELSE 0 END) AS cubre_dia_completo
            FROM sesiones s JOIN dias_rutina d ON d.id = s.dia_rutina_id
            LEFT JOIN asignaciones_rutina a ON a.alumno_id = s.alumno_id AND a.rutina_id = d.rutina_id
                AND a.fecha_inicio IS NOT NULL AND DATE(a.fecha_inicio) <= s.fecha
                AND (a.fecha_fin IS NULL OR DATE(a.fecha_fin) >= s.fecha)
            WHERE s.asignacion_id IS NULL AND s.estado_vinculo_asignacion = 'LEGADO_SIN_EVALUAR'
            GROUP BY s.id;
        UPDATE sesiones s JOIN gimnasio_s3_candidatas c ON c.sesion_id = s.id
            SET s.asignacion_id = IF(c.candidatas = 1 AND c.cubre_dia_completo = 1, c.asignacion_id, NULL),
                s.estado_vinculo_asignacion = CASE WHEN c.candidatas = 1 AND c.cubre_dia_completo = 1 THEN 'LEGADO_UNICO_POR_FECHA'
                    WHEN c.candidatas > 1 THEN 'LEGADO_AMBIGUO'
                    WHEN c.candidatas = 1 THEN 'LEGADO_FECHA_IMPRECISA' ELSE 'LEGADO_SIN_COINCIDENCIA' END;
        DROP TEMPORARY TABLE gimnasio_s3_candidatas;
        INSERT INTO gimnasio_schema_migrations(version_num, aplicada_en) VALUES (2, CURRENT_TIMESTAMP(6));
        COMMIT;
    END IF;
END$$
CALL gimnasio_s3_migrar()$$
DROP PROCEDURE gimnasio_s3_migrar$$
DROP PROCEDURE gimnasio_s3_columna$$
DELIMITER ;
