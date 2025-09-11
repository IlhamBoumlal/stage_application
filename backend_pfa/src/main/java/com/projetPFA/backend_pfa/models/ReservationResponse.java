package com.projetPFA.backend_pfa.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private String reservationId;
    private String message;
    private Integer pharmaciesNotified;
    private Boolean suggestExpandRadius;
    private List<String> pharmacyNames;

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPharmaciesNotified() {
        return pharmaciesNotified;
    }

    public void setPharmaciesNotified(Integer pharmaciesNotified) {
        this.pharmaciesNotified = pharmaciesNotified;
    }

    public Boolean getSuggestExpandRadius() {
        return suggestExpandRadius;
    }

    public void setSuggestExpandRadius(Boolean suggestExpandRadius) {
        this.suggestExpandRadius = suggestExpandRadius;
    }

    public List<String> getPharmacyNames() {
        return pharmacyNames;
    }

    public void setPharmacyNames(List<String> pharmacyNames) {
        this.pharmacyNames = pharmacyNames;
    }
}
