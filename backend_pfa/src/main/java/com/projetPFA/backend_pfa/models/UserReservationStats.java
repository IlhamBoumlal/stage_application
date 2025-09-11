package com.projetPFA.backend_pfa.models;

import lombok.*;


public class UserReservationStats {
    private String userName;
    private int reservationCount;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getReservationCount() {
        return reservationCount;
    }

    public void setReservationCount(int reservationCount) {
        this.reservationCount = reservationCount;
    }
    public UserReservationStats(String userName, int reservationCount) {
        this.userName = userName;
        this.reservationCount = reservationCount;
    }
}
