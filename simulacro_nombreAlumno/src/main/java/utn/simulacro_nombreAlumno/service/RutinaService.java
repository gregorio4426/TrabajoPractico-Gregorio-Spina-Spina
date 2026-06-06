package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.ReglaNegocioException;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.RutinaMapper;
import utn.simulacro_nombreAlumno.model.Ejercicio;
import utn.simulacro_nombreAlumno.model.Profesor;
import utn.simulacro_nombreAlumno.model.Rutina;
import utn.simulacro_nombreAlumno.model.request.RutinaRequest;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
import utn.simulacro_nombreAlumno.repository.RutinaRepository;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final RutinaMapper rutinaMapper;
    private final EjercicioService ejercicioService;
    private final ProfesorService profesorService;

    public Rutina findEntityById(long id) {
         return rutinaRepository.findById(id)
                 .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
     }

    public List<RutinaResponse> listarTodas (){
         List<Rutina> todas = rutinaRepository.findAll();
         return rutinaMapper.toLISTDto(todas);

     }

    public RutinaResponse crearRutina(RutinaRequest request) {
        if (request.getEjercicioIds() == null || request.getEjercicioIds().isEmpty()) {
            throw new ReglaNegocioException("La rutina debe tener al menos un ejercicio");
        }
        Profesor profesor = profesorService.findEntityById(request.getProfesorId());
        List<Ejercicio> ejercicios = new ArrayList<>();
        for (Long eid : request.getEjercicioIds()) {
            ejercicios.add(ejercicioService.findEntityById(eid));
        }
        Rutina rutina = rutinaMapper.toEntity(request);
        rutina.setEjercicios(ejercicios);
        rutina.setProfesor(profesor);
        return rutinaMapper.toDto(rutinaRepository.save(rutina));
    }

    public RutinaResponse updateRutina(Long id, RutinaRequest request) {
        Rutina rutina = findEntityById(id);
        if (request.getEjercicioIds() == null || request.getEjercicioIds().isEmpty()) {
            throw new ReglaNegocioException("La rutina debe tener al menos un ejercicio");
        }
        List<Ejercicio> ejercicios = new ArrayList<>();
        for (Long eid : request.getEjercicioIds()) {
            ejercicios.add(ejercicioService.findEntityById(eid));
        }
        rutina.setNombre(request.getNombre());
        rutina.setDescripcion(request.getDescripcion());
        rutina.setEjercicios(ejercicios);
        rutina.setProfesor(profesorService.findEntityById(request.getProfesorId()));
        return rutinaMapper.toDto(rutinaRepository.save(rutina));
    }

    public void deleteRutina(Long id) {
        rutinaRepository.delete(findEntityById(id));
    }
}


