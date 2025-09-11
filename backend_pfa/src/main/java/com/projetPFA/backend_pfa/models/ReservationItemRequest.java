package com.projetPFA.backend_pfa.models;

public class ReservationItemRequest {
    private String nameMedicament;
    private Integer quantite;
    private Double prixUnitaire;
    private String medicamentId; // CHAMP À AJOUTER
    // ... autres champs

    // Ajoutez le getter et le setter
    public String getMedicamentId() { return medicamentId; }
    public void setMedicamentId(String medicamentId) { this.medicamentId = medicamentId; }
    // Constructeur par défaut
    public ReservationItemRequest() {}

    public ReservationItemRequest(String nameMedicament, Integer quantite, Double prixUnitaire,String medicamentId) {
        this.nameMedicament = nameMedicament;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.medicamentId=medicamentId;
    }

    // Getters et Setters
    public String getNameMedicament() { return nameMedicament; }
    public void setNameMedicament(String nameMedicament) { this.nameMedicament = nameMedicament; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public Double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
}

