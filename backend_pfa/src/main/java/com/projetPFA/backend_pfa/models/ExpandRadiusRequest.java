package com.projetPFA.backend_pfa.models;

public class ExpandRadiusRequest {
    private Double newRadius;
    private String reason; // Optionnel

    // Getters et Setters
    public Double getNewRadius() { return newRadius; }
    public void setNewRadius(Double newRadius) { this.newRadius = newRadius; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
