package ru.tischenko.vk.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    private static final int MIN_SECRET_BYTES = 32;

    private final Key key;
    private final long expirationSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes; got " + bytes.length
                            + ". Provide a strong JWT_SECRET via environment.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationSeconds = expirationSeconds;
    }

    @PostConstruct
    void warnOnDefault() {
        // intentionally empty; secret strength is enforced in the constructor
    }

    public String generate(UserDetails userDetails) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }
}
