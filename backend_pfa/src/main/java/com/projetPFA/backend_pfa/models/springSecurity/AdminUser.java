package com.projetPFA.backend_pfa.models.springSecurity;

import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AdminUser implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@gmail.com";

        // On vérifie si l'admin existe déjà
        Optional<User> existingAdmin = userRepository.findByEmail(adminEmail);

        if (existingAdmin.isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setNom("Admin");
            admin.setPrenom("Super");
            // Le mot de passe doit être encodé !
            admin.setPassword(passwordEncoder.encode("admin123"));
            // Assurez-vous que votre modèle User peut gérer une liste de rôles
            admin.setRoles(List.of("ROLE_ADMIN", "ROLE_USER"));

            userRepository.save(admin);
            System.out.println(">>> Compte administrateur créé avec succès !");
        } else {
            System.out.println(">>> Compte administrateur déjà existant.");
        }
    }
}
