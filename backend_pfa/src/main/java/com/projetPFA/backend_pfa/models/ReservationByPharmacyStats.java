package com.projetPFA.backend_pfa.models;

public class ReservationByPharmacyStats {
    private String pharmacyName;
    private int reservations;

    public ReservationByPharmacyStats(String pharmacyName, int reservations) {
        this.pharmacyName = pharmacyName;
        this.reservations = reservations;
    }

    // Getters et Setters
    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public int getReservations() {
        return reservations;
    }

    public void setReservations(int reservations) {
        this.reservations = reservations;
    }
}
