package utn.simulacro_nombreAlumno.model.response;
public record PesoSugeridoResponse(Long diaEjercicioId,String ejercicioNombre,Double pesoSugerido,Integer repsSugeridas,Integer segundosSugeridos) {
 public PesoSugeridoResponse(Long id,String nombre,Double peso) {this(id,nombre,peso,null,null);}
}
