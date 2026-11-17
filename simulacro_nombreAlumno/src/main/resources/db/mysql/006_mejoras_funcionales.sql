-- Stop application writers and back up first. Apply after 005. Idempotent additive migration.
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s6_columna$$
CREATE PROCEDURE gimnasio_s6_columna(IN tabla VARCHAR(64), IN columna VARCHAR(64), IN definicion VARCHAR(512))
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tabla AND column_name=columna) THEN
  SET @gym_s6_sql=CONCAT('ALTER TABLE `',tabla,'` ADD COLUMN `',columna,'` ',definicion);
  PREPARE gym_s6_stmt FROM @gym_s6_sql; EXECUTE gym_s6_stmt; DEALLOCATE PREPARE gym_s6_stmt;
 END IF;
END$$
CALL gimnasio_s6_columna('dias_ejercicio','bloque_orden','INT NULL')$$
CALL gimnasio_s6_columna('dias_ejercicio','bloque_nombre','VARCHAR(100) NULL')$$
CALL gimnasio_s6_columna('dias_ejercicio','vueltas','INT NULL')$$
CALL gimnasio_s6_columna('dias_ejercicio','orden_en_bloque','INT NULL')$$
CALL gimnasio_s6_columna('dias_ejercicio','descanso_ejercicio_segundos','INT NULL')$$
CALL gimnasio_s6_columna('dias_ejercicio','descanso_vuelta_segundos','INT NULL')$$
CALL gimnasio_s6_columna('alumnos','objetivo_personalizado','VARCHAR(2000) NULL')$$
CALL gimnasio_s6_columna('sesiones','comentario','VARCHAR(2000) NULL')$$
CALL gimnasio_s6_columna('ejercicios','grupo_muscular_id','BIGINT NULL')$$
CALL gimnasio_s6_columna('ejercicios','nombre_normalizado','VARCHAR(60) NULL')$$
ALTER TABLE dias_ejercicio MODIFY grupo_muscular_snapshot VARCHAR(100) NULL$$
DROP PROCEDURE gimnasio_s6_columna$$
DELIMITER ;
CREATE TABLE IF NOT EXISTS grupos_musculares (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 normalizado VARCHAR(100) NOT NULL,
 CONSTRAINT uk_grupo_normalizado UNIQUE(normalizado)
) ENGINE=InnoDB;
INSERT IGNORE INTO grupos_musculares(nombre,normalizado) VALUES
 ('PECHO','pecho'),('ESPALDA','espalda'),('PIERNAS','piernas'),('BRAZOS','brazos'),('HOMBROS','hombros'),('ABDOMINALES','abdominales');
-- Los nombres existentes se conservan, incluso si ya había duplicados.
UPDATE ejercicios e JOIN grupos_musculares g ON g.normalizado=LOWER(e.grupo_muscular)
 SET e.grupo_muscular_id=g.id WHERE e.grupo_muscular_id IS NULL;
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s6_indices$$
CREATE PROCEDURE gimnasio_s6_indices()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='ejercicios' AND constraint_name='fk_ejercicio_grupo_catalogo') THEN
  ALTER TABLE ejercicios ADD CONSTRAINT fk_ejercicio_grupo_catalogo FOREIGN KEY(grupo_muscular_id) REFERENCES grupos_musculares(id);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='ejercicios' AND index_name='uk_ejercicio_nombre_normalizado') THEN
  CREATE UNIQUE INDEX uk_ejercicio_nombre_normalizado ON ejercicios(nombre_normalizado);
 END IF;
END$$
CALL gimnasio_s6_indices()$$
DROP PROCEDURE gimnasio_s6_indices$$
DELIMITER ;
INSERT IGNORE INTO gimnasio_schema_migrations(version_num,aplicada_en) VALUES(6,CURRENT_TIMESTAMP(6));
