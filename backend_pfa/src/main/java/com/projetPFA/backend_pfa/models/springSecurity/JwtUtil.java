package com.projetPFA.backend_pfa.models.springSecurity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret:mySecretKey123456789012345678901234567890}") String secret,
                   @Value("${app.jwt.expiration-ms:86400000}") String expirationMsStr) {
        // CORRECTION 1: Assurer que la clé fait au moins 32 caractères
        String finalSecret = secret;
        if (finalSecret.length() < 32) {
            finalSecret = finalSecret + "0123456789012345678901234567890123456789";
        }
        this.key = Keys.hmacShaKeyFor(finalSecret.getBytes());

        // CORRECTION 2: Parsing plus robuste de l'expiration
        long parsedExpiration;
        try {
            parsedExpiration = Long.parseLong(expirationMsStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            parsedExpiration = 86400000L; // 24 heures par défaut
        }
        this.expirationMs = parsedExpiration;
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername(), userDetails);
    }

    // CORRECTION 3: Méthode createToken séparée pour plus de clarté
    private String createToken(Map<String, Object> claims, String subject, UserDetails userDetails) {
        claims.put("sub", subject);
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // CORRECTION 4: Méthode générique pour extraire les claims
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // CORRECTION 5: Méthode extractAllClaims publique pour utilisation dans le contrôleur
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw e; // Re-lancer pour gestion spécifique
        } catch (MalformedJwtException | UnsupportedJwtException | SignatureException e) {
            throw new RuntimeException("Token JWT invalide", e);
        }
    }

    // CORRECTION 6: Surcharge pour validation avec UserDetails
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // CORRECTION 7: Méthode de validation sans UserDetails (pour le endpoint /validate)
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true; // Si on ne peut pas lire la date, considérer comme expiré
        }
    }

    // CORRECTION 8: Méthode pour obtenir le temps restant
    public long getTimeToExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.getTime() - new Date().getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    // CORRECTION 9: Méthode pour vérifier si le token sera bientôt expiré
    public boolean isTokenExpiringSoon(String token, long thresholdMs) {
        return getTimeToExpiration(token) < thresholdMs;
    }

    // CORRECTION 10: Méthode pour extraire les rôles
    public java.util.List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            @SuppressWarnings("unchecked")
            java.util.List<String> roles = (java.util.List<String>) claims.get("roles");
            return roles != null ? roles : java.util.List.of();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    // CORRECTION 11: Méthode pour debug (à supprimer en production)
    public void debugToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            System.out.println("Token Debug:");
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Issued At: " + claims.getIssuedAt());
            System.out.println("Expiration: " + claims.getExpiration());
            System.out.println("Roles: " + claims.get("roles"));
            System.out.println("Is Expired: " + isTokenExpired(token));
        } catch (Exception e) {
            System.err.println("Erreur debug token: " + e.getMessage());
        }
    }

    // CORRECTION 12: Méthode parseAllClaims dépréciée mais gardée pour compatibilité
    @Deprecated
    private Claims parseAllClaims(String token) {
        return extractAllClaims(token);
    }
}