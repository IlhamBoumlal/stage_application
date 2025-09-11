package com.projetPFA.backend_pfa.models;

import lombok.Data;
import lombok.*;


public class ReservationDailyStats {
    private String date; // Format "dd/MM"
    private int count;
    public ReservationDailyStats(String date, int count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
