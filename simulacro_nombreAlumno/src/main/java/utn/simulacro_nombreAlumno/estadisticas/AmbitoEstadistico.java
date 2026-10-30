package utn.simulacro_nombreAlumno.estadisticas;

/** Solo lo construye el service tras autorizar. Ambos IDs null significa gimnasio completo. */
record AmbitoEstadistico(Long alumnoId,Long profesorId) {}
