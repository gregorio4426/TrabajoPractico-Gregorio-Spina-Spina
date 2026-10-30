-- MySQL >= 8.0.16. Aplicación detenida; backup y migración 002 aplicados.
-- Conserva pesos, repeticiones, fechas, números e IDs históricos, incluso inválidos.
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s4_columna$$
CREATE PROCEDURE gimnasio_s4_columna(IN tabla VARCHAR(64), IN columna VARCHAR(64), IN definicion VARCHAR(512))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = columna) THEN
        SET @gimnasio_s4_sql = CONCAT('ALTER TABLE `', tabla, '` ADD COLUMN `', columna, '` ', definicion);
        PREPARE sentencia FROM @gimnasio_s4_sql; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;
    END IF;
END$$
DROP PROCEDURE IF EXISTS gimnasio_s4_migrar$$
CREATE PROCEDURE gimnasio_s4_migrar()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF NOT EXISTS (SELECT 1 FROM gimnasio_schema_migrations WHERE version_num = 2) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aplicar migración 002 antes de 003';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gimnasio_schema_migrations WHERE version_num = 3) THEN
        IF EXISTS (SELECT alumno_id FROM asignaciones_rutina WHERE activa = 1 GROUP BY alumno_id HAVING COUNT(*) > 1)
            OR EXISTS (SELECT 1 FROM asignaciones_rutina WHERE activa = 1 AND alumno_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Asignaciones activas duplicadas: resolver con evidencia antes de migrar';
        END IF;
        IF EXISTS (SELECT 1 FROM series s LEFT JOIN dias_ejercicio d ON d.id = s.dia_ejercicio_id LEFT JOIN ejercicios e ON e.id = d.ejercicio_id LEFT JOIN sesiones se ON se.id = s.sesion_id WHERE e.id IS NULL OR se.id IS NULL OR se.dia_rutina_id <> d.dia_rutina_id) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Series con referencias inconsistentes; no se puede deducir su identidad';
        END IF;
        CALL gimnasio_s4_columna('sesiones','fecha_corregida','DATE NULL');
        CALL gimnasio_s4_columna('sesiones','finalizada_en','DATETIME(6) NULL');
        CALL gimnasio_s4_columna('sesiones','actualizada_en','DATETIME(6) NULL');
        CALL gimnasio_s4_columna('sesiones','revision','BIGINT NOT NULL DEFAULT 0');
        CALL gimnasio_s4_columna('series','ejercicio_id','BIGINT NULL');
        CALL gimnasio_s4_columna('series','anulada','BIT(1) NOT NULL DEFAULT b''0''');
        CALL gimnasio_s4_columna('series','legado_exento','BIT(1) NOT NULL DEFAULT b''0''');
        CALL gimnasio_s4_columna('series','incidencia_legado','VARCHAR(128) NULL');
        CALL gimnasio_s4_columna('series','revision','BIGINT NOT NULL DEFAULT 0');
        CREATE TABLE IF NOT EXISTS auditoria_entrenamientos (
            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            sesion_id BIGINT NOT NULL, serie_id BIGINT NULL, actor_id BIGINT NOT NULL,
            accion VARCHAR(32) NOT NULL, motivo VARCHAR(500) NOT NULL,
            antes LONGTEXT NOT NULL, despues LONGTEXT NOT NULL, creada_en DATETIME(6) NOT NULL,
            CONSTRAINT fk_auditoria_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones(id),
            CONSTRAINT fk_auditoria_serie FOREIGN KEY (serie_id) REFERENCES series(id),
            CONSTRAINT fk_auditoria_actor FOREIGN KEY (actor_id) REFERENCES usuarios(id),
            INDEX ix_auditoria_sesion (sesion_id, id)
        ) ENGINE=InnoDB;
        START TRANSACTION;
        UPDATE series s JOIN dias_ejercicio d ON d.id = s.dia_ejercicio_id SET s.ejercicio_id = d.ejercicio_id WHERE s.ejercicio_id IS NULL;
        DROP TEMPORARY TABLE IF EXISTS gimnasio_s4_duplicadas;
        CREATE TEMPORARY TABLE gimnasio_s4_duplicadas AS SELECT sesion_id, ejercicio_id, numero_serie
            FROM series WHERE anulada = 0 GROUP BY sesion_id, ejercicio_id, numero_serie HAVING COUNT(*) > 1;
        UPDATE series s LEFT JOIN gimnasio_s4_duplicadas d ON d.sesion_id = s.sesion_id AND d.ejercicio_id = s.ejercicio_id AND d.numero_serie = s.numero_serie
            SET s.legado_exento = IF(s.numero_serie <= 0 OR s.reps_hechas IS NULL OR s.reps_hechas <= 0 OR s.peso_usado < 0 OR d.sesion_id IS NOT NULL, 1, 0),
            s.incidencia_legado = COALESCE(s.incidencia_legado, NULLIF(CONCAT_WS(';',
                IF(s.numero_serie <= 0 OR s.reps_hechas IS NULL OR s.reps_hechas <= 0 OR s.peso_usado < 0, 'VALORES_INVALIDOS', NULL),
                IF(d.sesion_id IS NOT NULL, 'NUMERO_DUPLICADO', NULL)), ''));
        DROP TEMPORARY TABLE gimnasio_s4_duplicadas;
        COMMIT;
        ALTER TABLE series MODIFY COLUMN ejercicio_id BIGINT NOT NULL, MODIFY COLUMN sesion_id BIGINT NOT NULL, MODIFY COLUMN dia_ejercicio_id BIGINT NOT NULL;
        ALTER TABLE sesiones MODIFY COLUMN alumno_id BIGINT NOT NULL, MODIFY COLUMN dia_rutina_id BIGINT NOT NULL;
        ALTER TABLE asignaciones_rutina MODIFY COLUMN alumno_id BIGINT NOT NULL, MODIFY COLUMN rutina_id BIGINT NOT NULL;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'series' AND constraint_name = 'fk_serie_ejercicio') THEN
            ALTER TABLE series ADD CONSTRAINT fk_serie_ejercicio FOREIGN KEY (ejercicio_id) REFERENCES ejercicios(id);
        END IF;
        CALL gimnasio_s4_columna('series','numero_serie_unica','INT GENERATED ALWAYS AS (CASE WHEN NOT anulada AND NOT legado_exento THEN numero_serie ELSE NULL END)');
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'series' AND index_name = 'uk_serie_numero') THEN
            ALTER TABLE series ADD CONSTRAINT uk_serie_numero UNIQUE (sesion_id, ejercicio_id, numero_serie_unica);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = 'series' AND constraint_name = 'ck_serie_valores') THEN
            ALTER TABLE series ADD CONSTRAINT ck_serie_valores CHECK (legado_exento OR (numero_serie > 0 AND reps_hechas IS NOT NULL AND reps_hechas > 0 AND (peso_usado IS NULL OR peso_usado >= 0)));
        END IF;
        CALL gimnasio_s4_columna('asignaciones_rutina','alumno_activo_id','BIGINT GENERATED ALWAYS AS (CASE WHEN activa THEN alumno_id ELSE NULL END)');
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'asignaciones_rutina' AND index_name = 'uk_alumno_asignacion_activa') THEN
            ALTER TABLE asignaciones_rutina ADD CONSTRAINT uk_alumno_asignacion_activa UNIQUE (alumno_activo_id);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'dias_rutina' AND index_name = 'ix_dia_orden') THEN
            CREATE INDEX ix_dia_orden ON dias_rutina(rutina_id, numero_orden, id);
        END IF;
        START TRANSACTION;
        INSERT INTO gimnasio_schema_migrations(version_num, aplicada_en) VALUES (3, CURRENT_TIMESTAMP(6));
        COMMIT;
    END IF;
END$$
CALL gimnasio_s4_migrar()$$
DROP PROCEDURE gimnasio_s4_migrar$$
DROP PROCEDURE gimnasio_s4_columna$$
DELIMITER ;
