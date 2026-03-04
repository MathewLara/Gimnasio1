package com.mathew.gimnasio.util;

import com.mathew.gimnasio.configuracion.ConfiguracionEnv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio JWT. Clave desde variable de entorno JWT_SECRET (mín. 32 caracteres).
 */
public class JwtService {

    private static final String SECRET_DEFAULT = "GimnasioAPI2026SecretKeyParaJWT_Minimo32Caracteres";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 horas

    private static SecretKey getKey() {
        String secret = ConfiguracionEnv.get("JWT_SECRET", SECRET_DEFAULT);
        if (SECRET_DEFAULT.equals(secret))
            System.err.println("Advertencia: JWT_SECRET no configurado; usando valor por defecto (no usar en producción).");
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generarToken(int idUsuario, int idRol, String usuario) {
        return Jwts.builder()
                .subject(String.valueOf(idUsuario))
                .claim("rol", idRol)
                .claim("usuario", usuario)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getKey())
                .compact();
    }

    public static Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String extraerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
