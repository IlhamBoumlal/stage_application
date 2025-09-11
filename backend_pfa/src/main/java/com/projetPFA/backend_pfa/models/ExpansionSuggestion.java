package com.projetPFA.backend_pfa.models;

public class ExpansionSuggestion {
    private Double radius;
    private Integer newPharmaciesCount;

    public ExpansionSuggestion() {}

    public ExpansionSuggestion(Double radius, Integer newPharmaciesCount) {
        this.radius = radius;
        this.newPharmaciesCount = newPharmaciesCount;
    }

    // Getters and Setters
    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }

    public Integer getNewPharmaciesCount() {
        return newPharmaciesCount;
    }

    public void setNewPharmaciesCount(Integer newPharmaciesCount) {
        this.newPharmaciesCount = newPharmaciesCount;
    }
}
