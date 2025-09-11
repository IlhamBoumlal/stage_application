package com.projetPFA.backend_pfa.models;

import java.util.List;

public class ExpandRadiusResponse {
    private String reservationId;
    private Double oldRadius;
    private Double newRadius;
    private Integer pharmaciesFound;
    private Integer pharmaciesNotified;
    private List<String> pharmacyNames;
    private String message;
    private Boolean success;
    private Boolean suggestFurtherExpansion;

    // Constructors
    public ExpandRadiusResponse() {}

    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public Double getOldRadius() {
        return oldRadius;
    }

    public void setOldRadius(Double oldRadius) {
        this.oldRadius = oldRadius;
    }

    public Double getNewRadius() {
        return newRadius;
    }

    public void setNewRadius(Double newRadius) {
        this.newRadius = newRadius;
    }

    public Integer getPharmaciesFound() {
        return pharmaciesFound;
    }

    public void setPharmaciesFound(Integer pharmaciesFound) {
        this.pharmaciesFound = pharmaciesFound;
    }

    public Integer getPharmaciesNotified() {
        return pharmaciesNotified;
    }

    public void setPharmaciesNotified(Integer pharmaciesNotified) {
        this.pharmaciesNotified = pharmaciesNotified;
    }

    public List<String> getPharmacyNames() {
        return pharmacyNames;
    }

    public void setPharmacyNames(List<String> pharmacyNames) {
        this.pharmacyNames = pharmacyNames;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getSuggestFurtherExpansion() {
        return suggestFurtherExpansion;
    }

    public void setSuggestFurtherExpansion(Boolean suggestFurtherExpansion) {
        this.suggestFurtherExpansion = suggestFurtherExpansion;
    }
}
