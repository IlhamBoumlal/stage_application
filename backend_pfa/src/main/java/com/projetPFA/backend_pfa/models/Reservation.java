package com.projetPFA.backend_pfa.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "reservations")
public class Reservation {
    @Id
    private String id;
    @Field("userId")
    private String userId;

    @Field("items")
    private List<ReservationItem> items;
    @Field("total")
    private Double total;
    @Field("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @Field("status") // NOUVEAU CHAMP : statut de la réservation
    private String status; // Ex: "en attente", "confirmée", "refusée", "annulée"
    @Field("updated_at")
    private LocalDateTime updatedAt;
    // NOUVEAUX CHAMPS pour la géolocalisation
    @Field("userLatitude")
    private Double userLatitude;
    @Field("userLongitude")
    private Double userLongitude;
    @Field("searchRadius")
    private Double searchRadius;
    private String confirmedByPharmacyId; // NOUVEAU CHAMP : ID de la pharmacie qui a confirmé
    @Field("expandedAt")
    private LocalDateTime expandedAt;

    // Getter et Setter pour expandedAt
    public LocalDateTime getExpandedAt() {
        return expandedAt;
    }

    public void setExpandedAt(LocalDateTime expandedAt) {
        this.expandedAt = expandedAt;
    }

    // Ajout d'une méthode utilitaire pour avoir le nom de date de réservation
    public LocalDateTime getDateReservation() {
        return this.createdAt; // Utilise createdAt comme date de réservation
    }
    private LocalDateTime reminderSentAt;

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }


    private boolean reminderSent = false;
    private LocalDateTime timeoutAt;

    // Getters et Setters
    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public LocalDateTime getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(LocalDateTime timeoutAt) {
        this.timeoutAt = timeoutAt;
    }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Constructeur mis à jour pour inclure le statut
    public Reservation(String id, String userId, List<ReservationItem> items, Double total, LocalDateTime createdAt, String status) {
        this.id = id;
        this.userId = userId;
       // this.pharmacyId = pharmacyId;
        this.items = items;
        this.total = total;
        this.createdAt = createdAt;
        this.status = status;
        this.confirmedByPharmacyId = null; // Initialisé à null

    }

    // NOUVEAU : Constructeur sans ID (pour la création)
    public Reservation(String userId, List<ReservationItem> items, Double total, LocalDateTime createdAt, String status) {
        this.userId = userId;
        //this.pharmacyId = pharmacyId;
        this.items = items;
        this.total = total;
        this.createdAt = createdAt;
        this.status = status;
        this.confirmedByPharmacyId = null; // Initialisé à null

    }

    public String getConfirmedByPharmacyId() {
        return confirmedByPharmacyId;
    }

    public void setConfirmedByPharmacyId(String confirmedByPharmacyId) {
        this.confirmedByPharmacyId = confirmedByPharmacyId;
    }

    // NOUVEAU constructeur avec géolocalisation
    public Reservation(String userId, List<ReservationItem> items, Double total, LocalDateTime createdAt,
                       String status, Double userLatitude, Double userLongitude, Double searchRadius) {
        this.userId = userId;
        this.items = items;
        this.total = total;
        this.createdAt = createdAt;
        this.status = status;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.searchRadius = searchRadius;
        this.confirmedByPharmacyId = null; // Initialisé à null

    }
    public Double getUserLatitude() { return userLatitude; }
    public void setUserLatitude(Double userLatitude) { this.userLatitude = userLatitude; }
    public Double getUserLongitude() { return userLongitude; }
    public void setUserLongitude(Double userLongitude) { this.userLongitude = userLongitude; }
    public Double getSearchRadius() { return searchRadius; }
    public void setSearchRadius(Double searchRadius) { this.searchRadius = searchRadius; }


    public Reservation() {

    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
/*
    public String getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(String pharmacyId) {
        this.pharmacyId = pharmacyId;
    }*/

    public List<ReservationItem> getItems() {
        return items;
    }

    public void setItems(List<ReservationItem> items) {
        this.items = items;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // NOUVEAU : Getter et Setter pour le statut
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}