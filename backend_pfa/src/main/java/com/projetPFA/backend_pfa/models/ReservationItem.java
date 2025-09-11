package com.projetPFA.backend_pfa.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "reservationsItems")
public class ReservationItem {
    @Id
    private String id;
    @Field("nameMedicament")
    private String nameMedicament;
    @Field("quantite")
    private Integer quantite;
    @Field("prixUnitaire")
    private Double prixUnitaire;
    @Field("sousTotal")
    private Double sousTotal;

    private String medicamentId; // CHAMP À AJOUTER

    // Ajoutez le getter et le setter
    public String getMedicamentId() { return medicamentId; }
    public void setMedicamentId(String medicamentId) { this.medicamentId = medicamentId; }

    public String getId() {
        return id;
    }
    public ReservationItem(){}
    public ReservationItem(String id, String medicamentId, String nameMedicament, Integer quantite, Double prixUnitaire, Double sousTotal) {
        this.id = id;
        this.medicamentId = medicamentId; // Ajout
        this.nameMedicament = nameMedicament;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.sousTotal = sousTotal;
    }


    public void setId(String id) {
        this.id = id;
    }

    public String getNameMedicament() {
        return nameMedicament;
    }

    public void setNameMedicament(String nameMedicament) {
        this.nameMedicament = nameMedicament;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
        if (this.prixUnitaire != null) {
            this.sousTotal = quantite * this.prixUnitaire;
        }
    }

    public Double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(Double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        if (this.quantite != null) {
            this.sousTotal = this.quantite * prixUnitaire;
        }
    }

    public Double getSousTotal() {
        return sousTotal;
    }

    public void setSousTotal(Double sousTotal) {
        this.sousTotal = sousTotal;
    }
}
