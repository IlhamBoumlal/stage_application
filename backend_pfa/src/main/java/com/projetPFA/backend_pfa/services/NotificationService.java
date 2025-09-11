package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.Pharmacie;
import com.projetPFA.backend_pfa.models.Reservation;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void notifyPharmacy(Pharmacie pharmacie, Reservation reservation) {
        // Implémentez l'envoi de notification par SMS/email/WhatsApp à la pharmacie
        // Ceci est un exemple simplifié

        String message = String.format(
                "Nouvelle réservation #%s. Médicaments: %s. Total: %.2f MAD. " +
                        "Lien pour confirmer: http://votre-domaine.com/pharmacy/reservations/%s/confirm " +
                        "Lien pour refuser: http://votre-domaine.com/pharmacy/reservations/%s/reject",
                reservation.getId(),
                reservation.getItems().stream()
                        .map(item -> item.getNameMedicament() + " (x" + item.getQuantite() + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Aucun"),
                reservation.getTotal(),
                reservation.getId(),
                reservation.getId()
        );

        // Envoyer le message à la pharmacie (à implémenter selon le canal choisi)
        System.out.println("Notification envoyée à " + pharmacie.getName() + ": " + message);

        // Exemple d'envoi d'email (pseudo-code)
        // emailService.send(pharmacie.getEmail(), "Nouvelle réservation", message);
    }

    public void notifyUserConfirmation(Reservation reservation) {
        // Notifier l'utilisateur que sa réservation est confirmée
        String message = String.format(
                "Votre réservation #%s a été confirmée par la pharmacie. " +
                        "Vous pouvez venir récupérer vos médicaments.",
                reservation.getId()
        );

        // Implémenter l'envoi de notification à l'utilisateur
        System.out.println("Notification de confirmation envoyée à l'utilisateur: " + message);
    }

    public void notifyUserRejection(Reservation reservation) {
        // Notifier l'utilisateur que sa réservation est refusée
        String message = String.format(
                "Votre réservation #%s a été refusée par la pharmacie. " +
                        "Nous vous suggérons de rechercher une autre pharmacie.",
                reservation.getId()
        );

        // Implémenter l'envoi de notification à l'utilisateur
        System.out.println("Notification de refus envoyée à l'utilisateur: " + message);
    }
    public void notifyUserReadyForPickup(Reservation reservation) {
        // Implémenter notification "Médicament prêt"
    }

    public void notifyUserCompleted(Reservation reservation) {
        // Implémenter notification "Réservation terminée"
    }

    public void notifyUserExpired(Reservation reservation) {
        // Implémenter notification "Réservation expirée"
    }
}