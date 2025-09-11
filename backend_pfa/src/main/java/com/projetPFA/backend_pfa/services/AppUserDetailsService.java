package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.models.springSecurity.AppUserDetails;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        // =========================================================
        // CORRECTION CRITIQUE :
        // On retourne une nouvelle instance de NOTRE classe AppUserDetails,
        // et non pas la classe org.springframework.security.core.userdetails.User.
        // C'est ce qui corrige la ClassCastException.
        // =========================================================
        return new AppUserDetails(user);
    }
}