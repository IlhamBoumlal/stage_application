package com.projetPFA.backend_pfa.models.springSecurity;

import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class Admin {
    @Bean
    public org.springframework.boot.CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            String email = "admin@pfa.com";
            if (!repo.existsByEmail(email)) {
                User admin = new User();
                admin.setNom("Admin");
                admin.setPrenom("Root");
                admin.setEmail(email);
                admin.setTelephone("0600000000");
                admin.setPassword(encoder.encode("Admin@123")); // à changer
                admin.setRoles(List.of("ADMIN")); // <- important
                repo.save(admin);
                System.out.println("✅ Admin créé : " + email + " / Admin@123");
            }
        };
    }
}
