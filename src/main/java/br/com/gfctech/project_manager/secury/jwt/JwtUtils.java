package br.com.gfctech.project_manager.secury.jwt;

import br.com.gfctech.project_manager.service.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
    private final SecretKey key;
    private final JwtParser parser;
    private final long expirationMs;

    public JwtUtils(@Value("${projeto.jwtSecret}") String secret,
                    @Value("${projeto.jwtExpirationMs}") long expirationMs,
                    Environment environment) {
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_MS deve ser positivo.");
        }
        if (secret.isBlank() && environment.acceptsProfiles(Profiles.of("demo", "test"))) {
            key = Jwts.SIG.HS512.key().build();
        } else {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 64) {
                throw new IllegalArgumentException("JWT_SECRET deve conter ao menos 64 bytes codificados em Base64.");
            }
            key = Keys.hmacShaKeyFor(keyBytes);
        }
        this.expirationMs = expirationMs;
        parser = Jwts.parser().verifyWith(key).build();
    }

    public String generateTokenFromUserDetailsImpl(UserDetailsImpl user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("id", user.getId())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("joinDate", user.getJoinDate())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public Claims parse(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
