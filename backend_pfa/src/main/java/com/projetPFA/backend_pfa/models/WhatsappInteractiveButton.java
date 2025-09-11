package com.projetPFA.backend_pfa.models;

/**
 * Classe pour représenter un bouton interactif WhatsApp
 */
public class WhatsappInteractiveButton {
    private String id;
    private String title;

    public WhatsappInteractiveButton() {}

    public WhatsappInteractiveButton(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}