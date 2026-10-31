package utn.simulacro_nombreAlumno.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService users;
    private final SecurityErrorWriter errors;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) { chain.doFilter(request,response); return; }
        try {
            if (java.util.Collections.list(request.getHeaders("Authorization")).size() != 1
                    || !header.regionMatches(true,0,"Bearer ",0,7) || header.substring(7).isBlank()) throw new BadCredentialsException("Token inválido");
            var claims = jwtService.parseClaims(header.substring(7).trim());
            CustomUserDetails details = users.loadById(jwtService.userId(claims));
            if (!jwtService.isTokenValid(claims, details)) throw new BadCredentialsException("Token inválido");
            var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException | AuthenticationException ex) {
            SecurityContextHolder.clearContext(); errors.unauthorized(response); return;
        }
        // Las excepciones de negocio no se convierten en errores JWT.
        chain.doFilter(request,response);
    }
}
