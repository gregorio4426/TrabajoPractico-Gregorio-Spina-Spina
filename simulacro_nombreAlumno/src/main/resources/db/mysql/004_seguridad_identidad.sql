-- MySQL >= 8.0.16. Detener escritores, verificar backup y aplicar 002/003 primero.
-- No modifica roles, contraseñas ni registros de entrenamiento.
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s5_columna$$
CREATE PROCEDURE gimnasio_s5_columna(IN tabla VARCHAR(64), IN columna VARCHAR(64), IN definicion VARCHAR(512))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = tabla AND column_name = columna) THEN
        SET @gimnasio_s5_sql = CONCAT('ALTER TABLE `', tabla, '` ADD COLUMN `', columna, '` ', definicion);
        PREPARE sentencia FROM @gimnasio_s5_sql; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;
    END IF;
END$$
DROP PROCEDURE IF EXISTS gimnasio_s5_migrar$$
CREATE PROCEDURE gimnasio_s5_migrar()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF NOT EXISTS (SELECT 1 FROM gimnasio_schema_migrations WHERE version_num = 3) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Aplicar migración 003 antes de 004';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gimnasio_schema_migrations WHERE version_num = 4) THEN
        IF EXISTS (SELECT 1 FROM usuarios WHERE email IS NULL OR TRIM(email) = '')
            OR EXISTS (SELECT LOWER(TRIM(email)) FROM usuarios GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Emails de acceso ambiguos: resolver con evidencia antes de migrar';
        END IF;
        IF EXISTS (SELECT 1 FROM alumnos p JOIN usuarios u ON u.id = p.usuario_id WHERE u.rol <> 'ALUMNO')
            OR EXISTS (SELECT 1 FROM profesores p JOIN usuarios u ON u.id = p.usuario_id WHERE u.rol <> 'PROFESOR') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Perfil incompatible con el rol: revisar relaciones antes de migrar';
        END IF;
        IF EXISTS (SELECT COALESCE(LOWER(TRIM(u.email)),LOWER(TRIM(p.email))) FROM alumnos p LEFT JOIN usuarios u ON u.id = p.usuario_id GROUP BY COALESCE(LOWER(TRIM(u.email)),LOWER(TRIM(p.email))) HAVING COUNT(*) > 1)
            OR EXISTS (SELECT COALESCE(LOWER(TRIM(u.email)),LOWER(TRIM(p.email))) FROM profesores p LEFT JOIN usuarios u ON u.id = p.usuario_id GROUP BY COALESCE(LOWER(TRIM(u.email)),LOWER(TRIM(p.email))) HAVING COUNT(*) > 1) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Email de perfil en conflicto: no se fusionan cuentas automáticamente';
        END IF;
        IF EXISTS (SELECT 1 FROM alumnos p JOIN usuarios u ON u.id=p.usuario_id JOIN alumnos otro ON otro.id<>p.id AND LOWER(TRIM(otro.email))=LOWER(TRIM(u.email)))
            OR EXISTS (SELECT 1 FROM profesores p JOIN usuarios u ON u.id=p.usuario_id JOIN profesores otro ON otro.id<>p.id AND LOWER(TRIM(otro.email))=LOWER(TRIM(u.email))) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Email de acceso ocupado por otro perfil: revisar antes de migrar';
        END IF;
        CALL gimnasio_s5_columna('usuarios','activo','BIT(1) NOT NULL DEFAULT b''1''');
        CALL gimnasio_s5_columna('usuarios','token_version','BIGINT NOT NULL DEFAULT 0');
        CALL gimnasio_s5_columna('alumnos','email_contacto','VARCHAR(150) NULL');
        CALL gimnasio_s5_columna('profesores','email_contacto','VARCHAR(150) NULL');
        CREATE TABLE IF NOT EXISTS seguridad_sistema (id BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
        START TRANSACTION;
        INSERT IGNORE INTO seguridad_sistema(id) VALUES (1);
        UPDATE alumnos p JOIN usuarios u ON u.id = p.usuario_id
            SET p.email_contacto = COALESCE(p.email_contacto, p.email)
            WHERE LOWER(TRIM(p.email)) <> LOWER(TRIM(u.email));
        UPDATE profesores p JOIN usuarios u ON u.id = p.usuario_id
            SET p.email_contacto = COALESCE(p.email_contacto, p.email)
            WHERE LOWER(TRIM(p.email)) <> LOWER(TRIM(u.email));
        UPDATE usuarios SET token_version = token_version + 1, email = LOWER(TRIM(email))
            WHERE BINARY email <> BINARY LOWER(TRIM(email));
        UPDATE alumnos p JOIN usuarios u ON u.id = p.usuario_id SET p.email = u.email;
        UPDATE profesores p JOIN usuarios u ON u.id = p.usuario_id SET p.email = u.email;
        INSERT INTO gimnasio_schema_migrations(version_num, aplicada_en) VALUES (4, CURRENT_TIMESTAMP(6));
        COMMIT;
    END IF;
END$$
CALL gimnasio_s5_migrar()$$
DROP PROCEDURE gimnasio_s5_migrar$$
DROP PROCEDURE gimnasio_s5_columna$$
DELIMITER ;
