package com.projetPFA.backend_pfa.models;

public class UserChoiceRequest {
    private String choice;
    private String additionalInfo; // Optionnel pour informations supplémentaires

    // Constructors
    public UserChoiceRequest() {}

    public UserChoiceRequest(String choice) {
        this.choice = choice;
    }

    // Getters and Setters
    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
