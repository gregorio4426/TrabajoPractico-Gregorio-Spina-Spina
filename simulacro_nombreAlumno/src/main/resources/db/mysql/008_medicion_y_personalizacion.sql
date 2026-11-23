-- Aplicar después de 007, con respaldo y escritores detenidos. No elimina resultados.
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s8_columnas$$
CREATE PROCEDURE gimnasio_s8_columnas()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='dias_ejercicio' AND column_name='tipo_medicion') THEN
  ALTER TABLE dias_ejercicio ADD COLUMN tipo_medicion VARCHAR(20) NOT NULL DEFAULT 'REPETICIONES';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='dias_ejercicio' AND column_name='segundos_sugeridos') THEN
  ALTER TABLE dias_ejercicio ADD COLUMN segundos_sugeridos INT NULL;
 END IF;
 ALTER TABLE dias_ejercicio MODIFY COLUMN reps_sugeridas INT NULL;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='series' AND column_name='tipo_medicion') THEN
  ALTER TABLE series ADD COLUMN tipo_medicion VARCHAR(20) NOT NULL DEFAULT 'REPETICIONES';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='series' AND column_name='segundos_hechos') THEN
  ALTER TABLE series ADD COLUMN segundos_hechos INT NULL;
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='series' AND constraint_name='ck_serie_valores') THEN
  ALTER TABLE series DROP CHECK ck_serie_valores;
 END IF;
 ALTER TABLE series ADD CONSTRAINT ck_serie_valores CHECK (legado_exento OR (numero_serie > 0 AND ((tipo_medicion='REPETICIONES' AND reps_hechas IS NOT NULL AND reps_hechas>0 AND segundos_hechos IS NULL) OR (tipo_medicion='SEGUNDOS' AND segundos_hechos IS NOT NULL AND segundos_hechos>0 AND reps_hechas IS NULL)) AND (peso_usado IS NULL OR peso_usado>=0)));
END$$
CALL gimnasio_s8_columnas()$$
DROP PROCEDURE gimnasio_s8_columnas$$
DELIMITER ;
CREATE TABLE IF NOT EXISTS reps_asignacion (
 asignacion_id BIGINT NOT NULL, dia_ejercicio_id BIGINT NOT NULL, valor INT NOT NULL,
 PRIMARY KEY(asignacion_id,dia_ejercicio_id),
 CONSTRAINT fk_reps_asignacion FOREIGN KEY(asignacion_id) REFERENCES asignaciones_rutina(id),
 CONSTRAINT fk_reps_asignacion_ejercicio FOREIGN KEY(dia_ejercicio_id) REFERENCES dias_ejercicio(id),
 CONSTRAINT ck_reps_asignacion_positivo CHECK(valor>0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS reps_sesion (
 sesion_id BIGINT NOT NULL, dia_ejercicio_id BIGINT NOT NULL, valor INT NOT NULL,
 PRIMARY KEY(sesion_id,dia_ejercicio_id),
 CONSTRAINT fk_reps_sesion FOREIGN KEY(sesion_id) REFERENCES sesiones(id),
 CONSTRAINT fk_reps_sesion_ejercicio FOREIGN KEY(dia_ejercicio_id) REFERENCES dias_ejercicio(id),
 CONSTRAINT ck_reps_sesion_positivo CHECK(valor>0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS segundos_asignacion (
 asignacion_id BIGINT NOT NULL, dia_ejercicio_id BIGINT NOT NULL, valor INT NOT NULL,
 PRIMARY KEY(asignacion_id,dia_ejercicio_id),
 CONSTRAINT fk_segundos_asignacion FOREIGN KEY(asignacion_id) REFERENCES asignaciones_rutina(id),
 CONSTRAINT fk_segundos_asignacion_ejercicio FOREIGN KEY(dia_ejercicio_id) REFERENCES dias_ejercicio(id),
 CONSTRAINT ck_segundos_asignacion_positivo CHECK(valor>0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS segundos_sesion (
 sesion_id BIGINT NOT NULL, dia_ejercicio_id BIGINT NOT NULL, valor INT NOT NULL,
 PRIMARY KEY(sesion_id,dia_ejercicio_id),
 CONSTRAINT fk_segundos_sesion FOREIGN KEY(sesion_id) REFERENCES sesiones(id),
 CONSTRAINT fk_segundos_sesion_ejercicio FOREIGN KEY(dia_ejercicio_id) REFERENCES dias_ejercicio(id),
 CONSTRAINT ck_segundos_sesion_positivo CHECK(valor>0)
) ENGINE=InnoDB;
INSERT IGNORE INTO gimnasio_schema_migrations(version_num,aplicada_en) VALUES(8,CURRENT_TIMESTAMP(6));
