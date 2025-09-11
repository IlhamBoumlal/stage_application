package com.projetPFA.backend_pfa.models.springSecurity;

import lombok.Data;
import java.util.List;

@Data
public class AuthResponse {
    private String token;
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private List<String> roles; // AJOUTEZ CETTE LIGNE

    public AuthResponse(String token, com.projetPFA.backend_pfa.models.User u){
        this.token = token;
        this.id = u.getId();
        this.nom = u.getNom();
        this.prenom = u.getPrenom();
        this.email = u.getEmail();
        this.telephone = u.getTelephone();
        this.roles = u.getRoles(); // AJOUTEZ CETTE LIGNE
    }
}