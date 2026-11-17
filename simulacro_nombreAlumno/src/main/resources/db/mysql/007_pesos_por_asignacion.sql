-- Aplicar después de 006, con backup y escritores detenidos. No borra datos.
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s7_columnas$$
CREATE PROCEDURE gimnasio_s7_columnas()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='asignaciones_rutina' AND column_name='revision') THEN
  ALTER TABLE asignaciones_rutina ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
 END IF;
END$$
CALL gimnasio_s7_columnas()$$
DROP PROCEDURE gimnasio_s7_columnas$$
DELIMITER ;
CREATE TABLE IF NOT EXISTS pesos_asignacion (
 asignacion_id BIGINT NOT NULL,
 dia_ejercicio_id BIGINT NOT NULL,
 peso_sugerido DOUBLE NOT NULL,
 PRIMARY KEY(asignacion_id,dia_ejercicio_id),
 CONSTRAINT fk_peso_asignacion FOREIGN KEY(asignacion_id) REFERENCES asignaciones_rutina(id),
 CONSTRAINT fk_peso_asignacion_ejercicio FOREIGN KEY(dia_ejercicio_id) REFERENCES dias_ejercicio(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS pesos_sesion (
 sesion_id BIGINT NOT NULL,
 dia_ejercicio_id BIGINT NOT NULL,
 peso_sugerido DOUBLE NOT NULL,
 PRIMARY KEY(sesion_id,dia_ejercicio_id),
 CONSTRAINT fk_peso_sesion FOREIGN KEY(sesion_id) REFERENCES sesiones(id),
 CONSTRAINT fk_peso_sesion_ejercicio FOREIGN KEY(dia_ejercicio_id) REFERENCES dias_ejercicio(id)
) ENGINE=InnoDB;
DELIMITER $$
DROP PROCEDURE IF EXISTS gimnasio_s7_copiar$$
CREATE PROCEDURE gimnasio_s7_copiar()
BEGIN
 -- El marcador impide restaurar un peso que el profesor haya limpiado después.
 IF NOT EXISTS(SELECT 1 FROM gimnasio_schema_migrations WHERE version_num=7) THEN
  INSERT IGNORE INTO pesos_asignacion(asignacion_id,dia_ejercicio_id,peso_sugerido)
   SELECT a.id,e.id,e.peso_sugerido FROM asignaciones_rutina a
   JOIN dias_rutina d ON d.rutina_id=a.rutina_id JOIN dias_ejercicio e ON e.dia_rutina_id=d.id
   WHERE e.peso_sugerido IS NOT NULL;
  INSERT IGNORE INTO pesos_sesion(sesion_id,dia_ejercicio_id,peso_sugerido)
   SELECT s.id,e.id,e.peso_sugerido FROM sesiones s JOIN dias_ejercicio e ON e.dia_rutina_id=s.dia_rutina_id
   WHERE e.peso_sugerido IS NOT NULL;
  INSERT INTO gimnasio_schema_migrations(version_num,aplicada_en) VALUES(7,CURRENT_TIMESTAMP(6));
 END IF;
END$$
CALL gimnasio_s7_copiar()$$
DROP PROCEDURE gimnasio_s7_copiar$$
DELIMITER ;
