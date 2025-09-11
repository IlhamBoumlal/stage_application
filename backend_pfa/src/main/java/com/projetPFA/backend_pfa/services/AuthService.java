package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDetails authenticate(String email, String password) {

        // --- 1️⃣ Vérifie si c'est l'admin ---
        if (email.equals("admin@gmail.com") && password.equals("admin123")) {
            return org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password(passwordEncoder.encode("admin123")) // encode le mot de passe admin
                    .roles("ADMIN")
                    .build();
        }

        // --- 2️⃣ Sinon cherche dans la base pour les utilisateurs normaux ---
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifie le mot de passe
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        // Transforme la liste de rôles en tableau pour Spring Security
        String[] rolesArray = user.getRoles().toArray(new String[0]);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(rolesArray)
                .build();
    }
}
