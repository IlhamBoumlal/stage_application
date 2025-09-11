package com.projetPFA.backend_pfa.models;

import java.util.List;

public class ReservationRequest {
    private String userId;
    private List<ReservationItemRequest> items;

    // Nouveaux champs pour la géolocalisation
    private double userLatitude;
    private double userLongitude;
    private Double searchRadius = 5.0; // Valeur par défaut en km

    // Constructeur par défaut
    public ReservationRequest() {}

    public ReservationRequest(String userId, List<ReservationItemRequest> items) {
        this.userId = userId;
        this.items = items;
    }

    // Constructeur complet
    public ReservationRequest(String userId, List<ReservationItemRequest> items,
                              double userLatitude, double userLongitude, Double searchRadius) {
        this.userId = userId;
        this.items = items;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.searchRadius = searchRadius;
    }

    // Getters et Setters existants
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<ReservationItemRequest> getItems() { return items; }
    public void setItems(List<ReservationItemRequest> items) { this.items = items; }

    // Nouveaux getters et setters pour la géolocalisation
    public double getUserLatitude() { return userLatitude; }
    public void setUserLatitude(double userLatitude) { this.userLatitude = userLatitude; }

    public double getUserLongitude() { return userLongitude; }
    public void setUserLongitude(double userLongitude) { this.userLongitude = userLongitude; }

    public Double getSearchRadius() { return searchRadius; }
    public void setSearchRadius(Double searchRadius) { this.searchRadius = searchRadius; }
}