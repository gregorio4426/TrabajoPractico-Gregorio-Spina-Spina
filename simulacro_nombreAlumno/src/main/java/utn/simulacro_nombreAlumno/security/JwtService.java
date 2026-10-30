package utn.simulacro_nombreAlumno.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.time.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {
    private final Key key;
    private final JwtProperties properties;
    private final Clock clock;
    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties; this.clock = clock;
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(properties.getSecretBase64()); }
        catch (RuntimeException ex) { throw new IllegalStateException("Configurar GYM_JWT_SECRET_BASE64 con una clave aleatoria de al menos 32 bytes en Base64"); }
        if (decoded.length < 32) throw new IllegalStateException("La clave JWT debe contener al menos 32 bytes");
        if (properties.getTtl() == null || properties.getTtl().isZero() || properties.getTtl().isNegative()
                || properties.getTtl().compareTo(Duration.ofHours(24)) > 0) throw new IllegalStateException("La duración JWT debe ser positiva y no superar 24 horas");
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) throw new IllegalStateException("Configurar emisor JWT");
        key = Keys.hmacShaKeyFor(decoded);
    }
    public String generateToken(UserDetails details) { return generateToken(Map.of(), details); }
    public String generateToken(Map<String, Object> extra, UserDetails details) {
        if (!(details instanceof CustomUserDetails user) || user.getUsuario().getId() == null || !user.isEnabled()) {
            throw new IllegalArgumentException("Se requiere un usuario persistido y activo");
        }
        Instant now = clock.instant();
        return Jwts.builder().setClaims(new HashMap<>(extra)).setSubject(user.getUsername())
                .setIssuer(properties.getIssuer()).claim("uid", user.getUsuario().getId())
                .claim("ver", user.getUsuario().getTokenVersion()).claim("roles", user.getAuthorities())
                .setIssuedAt(Date.from(now)).setExpiration(Date.from(now.plus(properties.getTtl())))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }
    public Claims parseClaims(String token) {
        var parsed = Jwts.parserBuilder().setSigningKey(key).requireIssuer(properties.getIssuer())
                .setClock(() -> Date.from(clock.instant())).build().parseClaimsJws(token);
        Claims c = parsed.getBody();
        if (!SignatureAlgorithm.HS256.getValue().equals(parsed.getHeader().getAlgorithm())
                || c.getExpiration() == null || !c.getExpiration().after(Date.from(clock.instant()))
                || c.getIssuedAt() == null || c.getIssuedAt().after(Date.from(clock.instant()))
                || c.getSubject() == null || c.getSubject().isBlank()) throw new JwtException("Token inválido");
        number(c, "uid", 1); number(c, "ver", 0);
        return c;
    }
    private long number(Claims c, String name, long minimum) {
        Object v = c.get(name);
        if (!(v instanceof Integer || v instanceof Long) || ((Number)v).longValue() < minimum) throw new JwtException("Token inválido");
        return ((Number)v).longValue();
    }
    public long userId(Claims c) { return number(c, "uid", 1); }
    public boolean isTokenValid(Claims c, CustomUserDetails details) {
        return details.isEnabled() && userId(c) == details.getUsuario().getId()
                && number(c,"ver",0) == details.getUsuario().getTokenVersion() && c.getSubject().equals(details.getUsername());
    }
    public boolean isTokenValid(String token, UserDetails details) {
        return details instanceof CustomUserDetails user && isTokenValid(parseClaims(token), user);
    }
    public String extractUsername(String token) { return parseClaims(token).getSubject(); }
    public <T> T extractClaim(String token, Function<Claims,T> resolver) { return resolver.apply(parseClaims(token)); }
}
