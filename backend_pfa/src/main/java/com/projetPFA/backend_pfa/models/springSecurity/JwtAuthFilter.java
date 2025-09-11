package com.projetPFA.backend_pfa.models.springSecurity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("🔍 JWT Filter - " + method + " " + requestPath);

        // CORRECTION PRINCIPALE: Ne pas traiter les endpoints publics
        if (shouldSkipFilter(requestPath, method)) {
            System.out.println("✅ Endpoint public - passage direct sans JWT");
            chain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                System.out.println("🔍 Token trouvé pour: " + username);
            } catch (Exception e) {
                System.out.println("❌ Erreur extraction username: " + e.getMessage());
            }
        } else {
            System.out.println("❌ Pas de token Bearer trouvé pour endpoint protégé");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                System.out.println("✅ Token valide pour: " + username);
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("❌ Token invalide pour: " + username);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Détermine si le filtre JWT doit être ignoré pour cette requête
     */
    private boolean shouldSkipFilter(String path, String method) {
        // Liste des endpoints publics - PAS BESOIN de JWT
        List<String> publicPaths = List.of(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/validate",    // IMPORTANT: validate doit être public !
                "/actuator",
                "/error"
        );

        // Vérifier les correspondances exactes
        for (String publicPath : publicPaths) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) {
                return true;
            }
        }

        // Endpoints GET spécifiques (pharmacies)
        if ("GET".equals(method) && path.startsWith("/api/pharmacies")) {
            return true;
        }

        // Requêtes OPTIONS pour CORS
        if ("OPTIONS".equals(method)) {
            return true;
        }

        return false;
    }
}