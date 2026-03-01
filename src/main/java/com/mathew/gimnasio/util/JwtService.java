package com.mathew.gimnasio.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * SERVICIO JWT (RF02)
 * Genera y valida tokens JWT para autorizar el acceso a endpoints protegidos.
 */
public class JwtService {

    private static final String SECRET = "GimnasioAPI2026SecretKeyParaJWT_Minimo32Caracteres";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 horas

    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un JWT con idUsuario, idRol y usuario (username).
     */
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

    /**
     * Valida el token y devuelve los Claims. Lanza excepción si es inválido o expirado.
     */
    public static Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el Bearer token del header Authorization.
     */
    public static String extraerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
