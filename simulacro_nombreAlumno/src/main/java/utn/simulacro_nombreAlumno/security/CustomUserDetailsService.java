package utn.simulacro_nombreAlumno.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import utn.simulacro_nombreAlumno.model.Usuario;
import utn.simulacro_nombreAlumno.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(EmailNormalizer.normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return new CustomUserDetails(usuario);
    }
    public CustomUserDetails loadById(Long id) {
        return new CustomUserDetails(usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas")));
    }
}
