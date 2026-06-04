package utn.simulacro_nombreAlumno.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.exception.RecursoNoEncontradoException;
import utn.simulacro_nombreAlumno.mapper.RutinaMapper;
import utn.simulacro_nombreAlumno.model.Alumno;
import utn.simulacro_nombreAlumno.model.Rutina;
import utn.simulacro_nombreAlumno.model.response.RutinaResponse;
import utn.simulacro_nombreAlumno.repository.RutinaRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final RutinaMapper rutinaMapper;

    public Rutina findEntityById(long id) {
        return rutinaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rutina no encontrada"));
    }

    public RutinaResponse findRutinaResponseById(long id) {
        return  rutinaMapper.toDto(findEntityById(id));
    }

    public List<RutinaResponse> listarTodas (){
        List<Rutina> todas = rutinaRepository.findAll();
        return rutinaMapper.toLISTDto(todas);

    }



}


