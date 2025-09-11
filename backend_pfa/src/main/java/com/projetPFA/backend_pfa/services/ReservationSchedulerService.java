package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.Reservation;
import com.projetPFA.backend_pfa.models.ReservationItem;
import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.services.reservations.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationSchedulerService.class);
    private static final int TIMEOUT_MINUTES = 3; // 3 minutes pour déclencher le rappel email

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    /**
     * Vérifie toutes les minutes si des réservations EN_ATTENTE dépassent le délai
     */
    @Scheduled(fixedRate = 60000)
    public void checkPendingReservationsForTimeout() {
        logger.info("🔍 Vérification timeout réservations - {}", LocalDateTime.now());

        try {
            List<Reservation> pendingReservations = reservationService.findByStatus("EN_ATTENTE");
            LocalDateTime now = LocalDateTime.now();

            logger.info("📋 Réservations EN_ATTENTE: {}", pendingReservations.size());

            for (Reservation reservation : pendingReservations) {
                long minutesElapsed = ChronoUnit.MINUTES.between(reservation.getDateReservation(), now);

                logger.info("📊 Réservation {} - {} minutes écoulées (seuil: {}, reminderSent: {})",
                        reservation.getId(), minutesElapsed, TIMEOUT_MINUTES, reservation.isReminderSent());

                if (minutesElapsed >= TIMEOUT_MINUTES && !reservation.isReminderSent()) {
                    logger.info("⏰ TIMEOUT déclenché pour réservation {}", reservation.getId());
                    handleReservationTimeout(reservation);
                }
            }

        } catch (Exception e) {
            logger.error("❌ Erreur vérification timeouts: {}", e.getMessage(), e);
        }
    }

    /**
     * Gère une réservation expirée : passe en attente d'options et envoie email de rappel
     */
    private void handleReservationTimeout(Reservation reservation) {
        try {
            User user = userService.findById(reservation.getUserId());
            if (user == null) {
                logger.warn("⚠️ Utilisateur non trouvé pour réservation {}", reservation.getId());
                return;
            }

            // Vérifier que l'utilisateur a un email
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                logger.warn("⚠️ Email manquant pour utilisateur {}", user.getId());
                reservation.setReminderSent(true); // Marquer comme traité pour éviter les boucles
                reservationService.save(reservation);
                return;
            }

            reservation.setReminderSent(true);
            reservation.setStatus("EN_ATTENTE_OPTIONS");
            reservation.setTimeoutAt(LocalDateTime.now());
            reservationService.save(reservation);

            // Envoyer email de rappel avec options
            sendReminderEmailToUser(user, reservation);

            logger.info("✅ Email de rappel avec options envoyé à {} pour réservation {}",
                    user.getEmail(), reservation.getId());

        } catch (Exception e) {
            logger.error("❌ Erreur handleReservationTimeout: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoie un email de rappel avec les options à l'utilisateur
     */
    private void sendReminderEmailToUser(User user, Reservation reservation) {
        try {
            long minutesWaiting = ChronoUnit.MINUTES.between(reservation.getCreatedAt(), LocalDateTime.now());

            emailNotificationService.sendUserReminderEmail(user, reservation, (int) minutesWaiting);

            logger.info("📧 Email rappel envoyé avec succès à: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Erreur envoi email rappel à {}: {}", user.getEmail(), e.getMessage());
            throw e;
        }
    }

    /**
     * Méthode alternative pour vérifier les rappels (peut être supprimée si redondante)
     */
    @Scheduled(fixedRate = 60000)
    public void checkForTimeoutReservations() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<Reservation> reservationsToRemind = reservationService.findAll().stream()
                    .filter(r -> "EN_ATTENTE".equals(r.getStatus()))
                    .filter(r -> r.getTimeoutAt() != null && now.isAfter(r.getTimeoutAt()))
                    .filter(r -> !r.isReminderSent())
                    .toList();

            logger.info("🔍 Vérification rappels alternatifs - {} réservations à traiter", reservationsToRemind.size());

            for (Reservation reservation : reservationsToRemind) {
                try {
                    sendReminderToUser(reservation);
                } catch (Exception e) {
                    logger.error("❌ Erreur envoi rappel pour réservation {}: {}", reservation.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la vérification alternative des rappels: {}", e.getMessage());
        }
    }

    private void sendReminderToUser(Reservation reservation) {
        try {
            User user = userService.findById(reservation.getUserId());
            if (user == null) {
                logger.warn("⚠️ Utilisateur non trouvé pour réservation: {}", reservation.getId());
                return;
            }

            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                logger.warn("⚠️ Email manquant pour utilisateur: {} ({})", user.getNom(), user.getId());
                reservation.setReminderSent(true);
                reservationService.save(reservation);
                return;
            }

            long minutesWaiting = java.time.Duration.between(reservation.getCreatedAt(), LocalDateTime.now()).toMinutes();

            logger.info("📧 Envoi rappel EMAIL à {} - {} minutes d'attente", user.getEmail(), minutesWaiting);

            emailNotificationService.sendUserReminderEmail(user, reservation, (int) minutesWaiting);

            reservation.setStatus("EN_ATTENTE_OPTIONS");
            reservation.setReminderSent(true);
            reservation.setReminderSentAt(LocalDateTime.now());
            reservationService.save(reservation);

            logger.info("✅ Rappel EMAIL envoyé avec succès à {} pour réservation {}", user.getEmail(), reservation.getId());

        } catch (Exception e) {
            logger.error("❌ Erreur envoi rappel EMAIL: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Méthode de test pour envoyer un rappel email immédiat
     */
    public boolean sendTestReminderEmail(String userId) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                logger.error("❌ Utilisateur non trouvé: {}", userId);
                return false;
            }

            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                logger.error("❌ Email manquant pour utilisateur: {}", userId);
                return false;
            }

            Optional<Reservation> reservationOpt = reservationService.findByUserId(userId)
                    .stream()
                    .filter(r -> "EN_ATTENTE".equals(r.getStatus()) || "EN_ATTENTE_OPTIONS".equals(r.getStatus()))
                    .findFirst();

            Reservation reservation;
            if (reservationOpt.isPresent()) {
                reservation = reservationOpt.get();
            } else {
                reservation = createTestReservation(userId);
                reservation = reservationService.save(reservation);
            }

            reservation.setStatus("EN_ATTENTE");
            reservation.setTimeoutAt(LocalDateTime.now().minusMinutes(1));
            reservation.setReminderSent(false);
            reservationService.save(reservation);

            sendReminderToUser(reservation);

            logger.info("✅ Rappel email de test envoyé à {} ({})", user.getEmail(), user.getNom());
            return true;

        } catch (Exception e) {
            logger.error("❌ Erreur envoi rappel email de test: {}", e.getMessage(), e);
            return false;
        }
    }

    private Reservation createTestReservation(String userId) {
        List<ReservationItem> testItems = List.of(
                createTestItem("PARACETAMOL_500MG", "Paracétamol 500mg", 2, 15.0),
                createTestItem("VITAMINE_C", "Vitamine C", 1, 25.0)
        );

        return new Reservation(
                userId,
                testItems,
                55.0,
                LocalDateTime.now(),
                "EN_ATTENTE",
                34.0331,
                -5.0003,
                5.0
        );
    }

    private ReservationItem createTestItem(String medicamentId, String name, int quantite, double prixUnitaire) {
        ReservationItem item = new ReservationItem();
        item.setMedicamentId(medicamentId);
        item.setNameMedicament(name);
        item.setQuantite(quantite);
        item.setPrixUnitaire(prixUnitaire);
        item.setSousTotal(quantite * prixUnitaire);
        return item;
    }

    /**
     * Traite le choix de l'utilisateur après clic sur les boutons email
     * Cette méthode peut être appelée par le controller lors de la gestion des choix
     */
    public void processUserEmailChoice(User user, Reservation reservation, String choice) {
        try {
            logger.info("📌 Traitement du choix {} pour réservation {} via email", choice, reservation.getId());

            if ("continue".equals(choice)) {
                // Continuer à attendre
                reservation.setStatus("EN_ATTENTE");
                reservation.setReminderSent(false);
                reservation.setTimeoutAt(LocalDateTime.now().plusMinutes(TIMEOUT_MINUTES));
                reservationService.save(reservation);

                // Envoyer email de confirmation du choix
                sendChoiceConfirmationEmail(user, reservation, "continue");

                logger.info("✅ Choix 'continuer' traité : réservation {} remise en attente", reservation.getId());

            } else if ("expand".equals(choice)) {
                // Élargir la recherche
                reservation.setStatus("EN_ATTENTE_ELARGIE");
                reservation.setExpandedAt(LocalDateTime.now());
                reservationService.save(reservation);

                // Envoyer email de confirmation du choix
                sendChoiceConfirmationEmail(user, reservation, "expand");

                logger.info("✅ Choix 'élargir' traité : recherche élargie pour réservation {}", reservation.getId());

            } else {
                logger.warn("⚠️ Choix invalide reçu: {}", choice);
            }

        } catch (Exception e) {
            logger.error("❌ Erreur processUserEmailChoice: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoie un email de confirmation du choix utilisateur
     */
    private void sendChoiceConfirmationEmail(User user, Reservation reservation, String choice) {
        try {
            String subject = "PharmaFès - Choix confirmé";
            String content;

            if ("continue".equals(choice)) {
                content = String.format(
                        "Bonjour %s,<br><br>" +
                                "✅ <strong>Votre choix a été confirmé !</strong><br><br>" +
                                "Nous continuons à rechercher une pharmacie pour votre réservation n°%s.<br>" +
                                "Vous serez notifié par email dès qu'une pharmacie confirmera votre demande.<br><br>" +
                                "Merci de votre patience !<br><br>" +
                                "L'équipe PharmaFès",
                        user.getNom() + " " + user.getPrenom(), reservation.getId()
                );
            } else {
                content = String.format(
                        "Bonjour %s,<br><br>" +
                                "✅ <strong>Votre recherche a été élargie !</strong><br><br>" +
                                "Votre réservation n°%s fait maintenant l'objet d'une recherche dans un rayon de %.1f km.<br>" +
                                "De nouvelles pharmacies ont été contactées.<br><br>" +
                                "Vous serez notifié par email dès qu'une pharmacie confirmera votre demande.<br><br>" +
                                "Merci de votre patience !<br><br>" +
                                "L'équipe PharmaFès",
                        user.getNom() + " " + user.getPrenom(),
                        reservation.getId(),
                        reservation.getSearchRadius()
                );
            }

            emailNotificationService.sendCustomEmail(user.getEmail(), subject, content);
            logger.info("📧 Email de confirmation de choix envoyé à: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Erreur envoi email de confirmation de choix: {}", e.getMessage());
        }
    }
}