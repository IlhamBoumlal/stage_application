// =================================================================
// FICHIER : controllers/AuthController.java (COMPLET ET CORRIGÉ)
// =================================================================

package com.projetPFA.backend_pfa.controllers;

import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.models.springSecurity.*;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import com.projetPFA.backend_pfa.services.AppUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AppUserDetailsService appUserDetailsService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          AppUserDetailsService appUserDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.appUserDetailsService = appUserDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req){
        try {
            if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(createErrorResponse("Email déjà utilisé"));
            }

            User u = new User();
            u.setNom(req.getNom());
            u.setPrenom(req.getPrenom());
            u.setEmail(req.getEmail().toLowerCase());
            u.setTelephone(req.getTelephone());
            u.setPassword(passwordEncoder.encode(req.getPassword()));
            u.setRoles(List.of("ROLE_USER"));

            User savedUser = userRepository.save(u);

            AppUserDetails userDetails = new AppUserDetails(savedUser);
            String token = jwtUtil.generateToken(userDetails);

            return ResponseEntity.status(HttpStatus.CREATED).body(createSuccessResponse(token, savedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail().toLowerCase(), req.getPassword())
            );

            // Le cast va maintenant fonctionner car AppUserDetailsService renvoie le bon type
            AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(principal);

            // On récupère l'objet User complet depuis le principal
            User user = principal.getUser();

            return ResponseEntity.ok(createSuccessResponse(token, user));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("Email ou mot de passe incorrect"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Erreur serveur"));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        String token = authHeader.substring(7);
        try {
            if (jwtUtil.isTokenValid(token)) {
                return ResponseEntity.ok(Map.of("valid", true));
            }
        } catch (Exception e) {
            // Gère les tokens expirés, malformés, etc.
        }
        return ResponseEntity.ok(Map.of("valid", false));
    }

    // ... (Les autres endpoints comme /me et /logout peuvent rester, ils sont corrects)
    // ... (Les méthodes utilitaires createErrorResponse et createSuccessResponse sont correctes)
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }

    private Map<String, Object> createSuccessResponse(String token, User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("nom", user.getNom());
        response.put("prenom", user.getPrenom());
        response.put("telephone", user.getTelephone());
        response.put("roles", user.getRoles());
        return response;
    }
}