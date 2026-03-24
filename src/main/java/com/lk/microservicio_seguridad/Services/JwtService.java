package com.lk.microservicio_seguridad.Services;

import com.lk.microservicio_seguridad.Repositories.UserRoleRepository;
import com.lk.microservicio_seguridad.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 🔐 Genera la clave segura a partir del secret
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 🔑 Genera el token JWT con información del usuario
     */
    public String generateToken(User theUser) {
        try {
            logger.info("Generando token JWT para usuario: {}", theUser.getEmail());

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            Map<String, Object> claims = new HashMap<>();
            claims.put("id", theUser.getId());
            claims.put("name", theUser.getName());
            claims.put("email", theUser.getEmail());

            // 🔥 IMPORTANTE: incluir rol en el token
            String roleName = "";

            logger.info("Buscando roles para userId: {}", theUser.getId());
            var roles = userRoleRepository.findByUserId(theUser.getId());
            logger.info("Roles encontrados: {}", roles.size());

            if (!roles.isEmpty() && roles.get(0).getRole() != null) {
                roleName = roles.get(0).getRole().getName(); // toma el primer rol
            }

            claims.put("role", roleName);
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(theUser.getName())
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();

            logger.info("Token JWT generado exitosamente para: {}", theUser.getEmail());
            return token;
        } catch (Exception e) {
            logger.error("Error generando token JWT para usuario: {}", theUser.getEmail(), e);
            throw new RuntimeException("Error interno al generar token", e);
        }
    }

    /**
     * ✅ Valida el token (firma + expiración)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 👤 Extrae el usuario desde el token
     */
    public User getUserFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            User user = new User();
            user.setId((String) claims.get("id"));
            user.setName((String) claims.get("name"));
            user.setEmail((String) claims.get("email"));

            return user;

        } catch (Exception e) {
            return null;
        }
    }
}