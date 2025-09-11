// =================================================================
// FICHIER : models/springSecurity/AppUserDetails.java (COMPLET)
// =================================================================

package com.projetPFA.backend_pfa.models.springSecurity;

import com.projetPFA.backend_pfa.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class AppUserDetails implements UserDetails {
    private final User user;

    public AppUserDetails(User user){ this.user = user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Gère le cas où les rôles sont nuls pour éviter les erreurs
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return user.getRoles().stream()
                .map(SimpleGrantedAuthority::new) // On garde le rôle tel quel (ex: "ROLE_USER")
                .collect(Collectors.toList());
    }

    @Override public String getPassword() { return user.getPassword(); }
    @Override public String getUsername() { return user.getEmail(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // Méthode cruciale pour récupérer l'objet User complet dans le contrôleur
    public User getUser() { return user; }
}