package com.projetPFA.backend_pfa.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection= "medicaments")
public class Medicament {
    @Id
    private String id;

    @Field("STATUT AMM")
    private String statutAMM;

    @Field("STATUT COMMERCIALISATION")
    private String statutCommercialisation;

    @Field("SPECIALITE")
    private String specialite;

    @Field("DOSAGE")
    private String dosage;

    @Field("FORME")
    private String forme;

    @Field("PRESENTATION")
    private String presentation;

    @Field("PP GN")
    private String ppGn;

    @Field("SUBSTANCE ACTIVE")
    private String substanceActive;

    @Field("CLASSE THERAPEUTIQUE")
    private String classeTherapeutique;

    @Field("EPI")
    private String epi;

    @Field("PPV")
    private Double ppv;

    @Field("PH")
    private String ph;

    @Field("PFHT")
    private String pfht;

    @Field("CODE")
    private String code;

    @Field("TVA")
    private String tva;

    @Field("quantite")
    private int quantite;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public String getTva() {
        return tva;
    }

    public void setTva(String tva) {
        this.tva = tva;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPfht() {
        return pfht;
    }

    public void setPfht(String pfht) {
        this.pfht = pfht;
    }

    public String getPh() {
        return ph;
    }

    public void setPh(String ph) {
        this.ph = ph;
    }

    public Double getPpv() {
        return ppv;
    }

    public void setPpv(Double ppv) {
        this.ppv = ppv;
    }

    public String getEpi() {
        return epi;
    }

    public void setEpi(String epi) {
        this.epi = epi;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getForme() {
        return forme;
    }

    public void setForme(String forme) {
        this.forme = forme;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getStatutCommercialisation() {
        return statutCommercialisation;
    }

    public void setStatutCommercialisation(String statutCommercialisation) {
        this.statutCommercialisation = statutCommercialisation;
    }

    public String getStatutAMM() {
        return statutAMM;
    }

    public void setStatutAMM(String statutAMM) {
        this.statutAMM = statutAMM;
    }

    public String getPresentation() {
        return presentation;
    }

    public void setPresentation(String presentation) {
        this.presentation = presentation;
    }

    public String getPpGn() {
        return ppGn;
    }

    public void setPpGn(String ppGn) {
        this.ppGn = ppGn;
    }

    public String getSubstanceActive() {
        return substanceActive;
    }

    public void setSubstanceActive(String substanceActive) {
        this.substanceActive = substanceActive;
    }

    public String getClasseTherapeutique() {
        return classeTherapeutique;
    }

    public void setClasseTherapeutique(String classeTherapeutique) {
        this.classeTherapeutique = classeTherapeutique;
    }
}
