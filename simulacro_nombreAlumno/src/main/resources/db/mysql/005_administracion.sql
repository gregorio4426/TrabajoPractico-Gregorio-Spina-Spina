-- MySQL 8.4. Detener todos los escritores y verificar un backup antes de aplicar.
-- No borra perfiles, usuarios, vínculos ni entrenamientos. Las anomalías requieren revisión humana.
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s6_migrar$$
CREATE PROCEDURE gimnasio_s6_migrar()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF NOT EXISTS (SELECT 1 FROM gimnasio_schema_migrations WHERE version_num=4) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Aplicar migración 004 antes de 005';
    END IF;
    IF EXISTS (SELECT alumno_id,profesor_id FROM alumnos_profesore GROUP BY alumno_id,profesor_id HAVING COUNT(*)>1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Vinculaciones duplicadas: revisar con evidencia antes de migrar';
    END IF;
    IF EXISTS (SELECT 1 FROM usuarios u LEFT JOIN alumnos a ON a.usuario_id=u.id WHERE u.rol='ALUMNO' AND a.id IS NULL)
        OR EXISTS (SELECT 1 FROM usuarios u LEFT JOIN profesores p ON p.usuario_id=u.id WHERE u.rol='PROFESOR' AND p.id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cuenta sin perfil para su rol: regularizar con evidencia antes de migrar';
    END IF;
    IF EXISTS (SELECT 1 FROM alumnos a JOIN usuarios u ON u.id=a.usuario_id WHERE BINARY a.email<>BINARY u.email)
        OR EXISTS (SELECT 1 FROM profesores p JOIN usuarios u ON u.id=p.usuario_id WHERE BINARY p.email<>BINARY u.email) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Emails de identidad inconsistentes: revisar antes de migrar';
    END IF;
    CREATE TABLE IF NOT EXISTS auditoria_administrativa (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        actor_id BIGINT NOT NULL, usuario_id BIGINT NULL,
        accion VARCHAR(32) NOT NULL, motivo VARCHAR(500) NOT NULL,
        antes LONGTEXT NOT NULL, despues LONGTEXT NOT NULL, creada_en DATETIME(6) NOT NULL,
        INDEX ix_auditoria_admin_usuario(usuario_id,id),
        CONSTRAINT fk_auditoria_admin_actor FOREIGN KEY(actor_id) REFERENCES usuarios(id),
        CONSTRAINT fk_auditoria_admin_usuario FOREIGN KEY(usuario_id) REFERENCES usuarios(id)
    ) ENGINE=InnoDB;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='alumnos_profesore' AND index_name='uk_alumno_profesor') THEN
        ALTER TABLE alumnos_profesore ADD CONSTRAINT uk_alumno_profesor UNIQUE(alumno_id,profesor_id);
    END IF;
    START TRANSACTION;
    INSERT IGNORE INTO gimnasio_schema_migrations(version_num,aplicada_en) VALUES(5,CURRENT_TIMESTAMP(6));
    COMMIT;
END$$
CALL gimnasio_s6_migrar()$$
DROP PROCEDURE gimnasio_s6_migrar$$
DELIMITER ;
