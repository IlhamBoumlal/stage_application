package com.projetPFA.backend_pfa.models;

public class DashboardStatistiquesDTO {
    private long totalUsers;
    private long totalPharmacies;
    private long totalReservations;
    private long totalMedicaments;


    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalPharmacies() {
        return totalPharmacies;
    }

    public void setTotalPharmacies(long totalPharmacies) {
        this.totalPharmacies = totalPharmacies;
    }

    public long getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(long totalReservations) {
        this.totalReservations = totalReservations;
    }

    public long getTotalMedications() {
        return totalMedicaments;
    }

    public void setTotalMedicaments(long totalMedications) {
        this.totalMedicaments = totalMedications;
    }
}
