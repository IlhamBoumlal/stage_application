package com.projetPFA.backend_pfa.services.reservations;

import com.projetPFA.backend_pfa.models.Pharmacie;
import com.projetPFA.backend_pfa.models.Reservation;
import com.projetPFA.backend_pfa.models.ReservationItem;
import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.services.ReservationService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class EmailNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    // ============= EMAILS AUX PHARMACIES =============

    public void sendReservationNotification(Pharmacie pharmacie, Reservation reservation, User user) {
        try {
            if (pharmacie.getEmail() != null) {
                String emailToUse = pharmacie.getEmail().replaceAll("[\\p{C}\\s]+", "");
                pharmacie.setEmail(emailToUse);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(pharmacie.getEmail());
            helper.setSubject("Nouvelle demande de réservation - PharmaFès");

            String htmlContent = buildEmailContent(pharmacie, reservation, user);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            logger.error("Erreur envoi email à " + pharmacie.getEmail() + ": " + e.getMessage());
            throw new RuntimeException("Échec envoi email pharmacie", e);
        }
    }

    // ============= NOUVEAUX EMAILS AUX UTILISATEURS =============

    /**
     * Email de confirmation de création de réservation envoyé à l'utilisateur
     */
    public void sendReservationCreatedConfirmation(User user, Reservation reservation, int pharmaciesContacted) {
        try {
            String subject = "PharmaFès - Votre réservation a été créée";
            String htmlContent = buildReservationCreatedEmail(user, reservation, pharmaciesContacted);

            sendCustomEmail(user.getEmail(), subject, htmlContent);
            logger.info("Email confirmation création envoyé à: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Erreur envoi confirmation création: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Email de confirmation envoyé à l'utilisateur quand une pharmacie accepte
     * Remplace la notification WhatsApp
     */
    public boolean sendConfirmationEmail(User user, Pharmacie pharmacie, Reservation reservation) {
        try {
            String subject = "🎉 PharmaFès - Votre réservation est confirmée !";
            String htmlContent = buildConfirmationEmail(user, pharmacie, reservation);

            sendCustomEmail(user.getEmail(), subject, htmlContent);
            logger.info("Email confirmation envoyé à: {} pour pharmacie: {}", user.getEmail(), pharmacie.getName());
            return true;

        } catch (Exception e) {
            logger.error("Erreur envoi email confirmation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Email de rappel avec options - remplace les messages WhatsApp
     */
    public void sendUserReminderEmail(User user, Reservation reservation, int minutesWaiting) {
        try {
            String subject = "PharmaFès - Votre réservation en attente - Que souhaitez-vous faire ?";

            String continueToken = generateSecureToken(reservation.getId(), "CONTINUE");
            String expandToken = generateSecureToken(reservation.getId(), "EXPAND");

            String continueUrl = baseUrl + "/api/reservations/" + reservation.getId() + "/user-choice?choice=continue&token=" + continueToken;
            String expandUrl = baseUrl + "/api/reservations/" + reservation.getId() + "/user-choice?choice=expand&token=" + expandToken;

            String emailContent = buildUserReminderEmailContent(user, reservation, minutesWaiting, continueUrl, expandUrl);

            sendCustomEmail(user.getEmail(), subject, emailContent);
            logger.info("Email rappel avec options envoyé à: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Erreur envoi email rappel: {}", e.getMessage());
            throw new RuntimeException("Échec envoi email rappel", e);
        }
    }

    // ============= CONSTRUCTEURS D'EMAILS =============

    /**
     * Construction email de confirmation de création de réservation
     */
    private String buildReservationCreatedEmail(User user, Reservation reservation, int pharmaciesContacted) {
        StringBuilder items = new StringBuilder();
        for (ReservationItem item : reservation.getItems()) {
            items.append(String.format(
                    "<tr><td>%s</td><td style='text-align: center;'>%d</td><td style='text-align: right;'>%.2f DH</td></tr>",
                    item.getNameMedicament(), item.getQuantite(), item.getSousTotal()
            ));
        }

        return String.format(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +

                        "<!-- Header -->" +
                        "<div style='background: #4CAF50; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                        "<h1 style='margin: 0; font-size: 28px;'>✅ PharmaFès</h1>" +
                        "<p style='margin: 5px 0 0 0; font-size: 18px;'>Réservation créée avec succès !</p>" +
                        "</div>" +

                        "<!-- Corps -->" +
                        "<div style='background: white; padding: 30px; border: 1px solid #ddd;'>" +
                        "<h2 style='color: #4CAF50;'>Bonjour %s,</h2>" +

                        "<div style='background: #e8f5e9; border-left: 4px solid #4CAF50; padding: 15px; margin: 20px 0;'>" +
                        "<p style='margin: 0;'><strong>🎯 Votre demande de réservation a été transmise à %d pharmacie(s) proche(s) !</strong></p>" +
                        "</div>" +

                        "<h3>📋 Détails de votre réservation :</h3>" +
                        "<table style='width: 100%%; border-collapse: collapse; margin: 20px 0;'>" +
                        "<tr style='background: #f8f9fa;'>" +
                        "<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>Article</th>" +
                        "<th style='padding: 12px; text-align: center; border: 1px solid #ddd;'>Quantité</th>" +
                        "<th style='padding: 12px; text-align: right; border: 1px solid #ddd;'>Prix</th>" +
                        "</tr>%s" +
                        "<tr style='background: #4CAF50; color: white; font-weight: bold;'>" +
                        "<td colspan='2' style='padding: 12px; border: 1px solid #ddd;'>Total</td>" +
                        "<td style='padding: 12px; text-align: right; border: 1px solid #ddd;'>%.2f DH</td>" +
                        "</tr></table>" +

                        "<div style='background: #fff3cd; border: 1px solid #ffeaa7; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                        "<h3 style='margin-top: 0; color: #856404;'>⏰ Prochaines étapes :</h3>" +
                        "<ul style='margin: 0;'>" +
                        "<li>Les pharmacies vont examiner votre demande</li>" +
                        "<li>Vous recevrez un email dès qu'une pharmacie confirme</li>" +
                        "<li>Si aucune réponse dans 3 minutes, nous vous proposerons d'autres options</li>" +
                        "</ul></div>" +

                        "<p style='text-align: center; color: #666; margin-top: 30px;'>" +
                        "Merci de faire confiance à PharmaFès !<br>" +
                        "Numéro de réservation : <strong>%s</strong></p>" +

                        "</div></div></body></html>",

                user.getNom() + " " + user.getPrenom(),
                pharmaciesContacted,
                items.toString(),
                reservation.getTotal(),
                reservation.getId()
        );
    }

    /**
     * Construction email de confirmation (remplace WhatsApp)
     */
    private String buildConfirmationEmail(User user, Pharmacie pharmacie, Reservation reservation) {
        StringBuilder items = new StringBuilder();
        for (ReservationItem item : reservation.getItems()) {
            items.append(String.format(
                    "<tr><td>%s</td><td style='text-align: center;'>%d</td><td style='text-align: right;'>%.2f DH</td></tr>",
                    item.getNameMedicament(), item.getQuantite(), item.getSousTotal()
            ));
        }

        return String.format(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +

                        "<!-- Header Success -->" +
                        "<div style='background: linear-gradient(135deg, #4CAF50, #45a049); color: white; padding: 40px; text-align: center; border-radius: 15px 15px 0 0;'>" +
                        "<h1 style='margin: 0; font-size: 32px;'>🎉 RÉSERVATION CONFIRMÉE !</h1>" +
                        "<p style='margin: 10px 0 0 0; font-size: 20px; opacity: 0.9;'>Votre commande vous attend</p>" +
                        "</div>" +

                        "<!-- Corps Principal -->" +
                        "<div style='background: white; padding: 40px; border: 1px solid #ddd;'>" +
                        "<h2 style='color: #4CAF50; margin-top: 0;'>Félicitations %s !</h2>" +

                        "<!-- Alerte Succès -->" +
                        "<div style='background: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 20px; border-radius: 8px; margin: 25px 0;'>" +
                        "<h3 style='margin-top: 0;'>✅ Bonne nouvelle !</h3>" +
                        "<p style='margin: 0; font-size: 18px;'><strong>La pharmacie \"%s\" a confirmé votre réservation.</strong></p>" +
                        "<p style='margin: 10px 0 0 0;'>Vos médicaments sont prêts et vous attendent !</p>" +
                        "</div>" +

                        "<!-- Détails Pharmacie -->" +
                        "<div style='background: #e3f2fd; border-left: 5px solid #2196F3; padding: 20px; margin: 25px 0;'>" +
                        "<h3 style='margin-top: 0; color: #1976D2;'>🏥 Informations Pharmacie</h3>" +
                        "<p><strong>Nom :</strong> %s</p>" +
                        "<p><strong>Email :</strong> %s</p>" +
                        "<p><strong>Réservation :</strong> %s</p>" +
                        "</div>" +

                        "<!-- Récapitulatif Commande -->" +
                        "<h3 style='color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px;'>📋 Votre commande :</h3>" +
                        "<table style='width: 100%%; border-collapse: collapse; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                        "<tr style='background: #4CAF50; color: white;'>" +
                        "<th style='padding: 15px; text-align: left;'>Médicament</th>" +
                        "<th style='padding: 15px; text-align: center;'>Quantité</th>" +
                        "<th style='padding: 15px; text-align: right;'>Prix</th>" +
                        "</tr>%s" +
                        "<tr style='background: #f8f9fa; font-weight: bold; font-size: 18px;'>" +
                        "<td colspan='2' style='padding: 15px; border-top: 2px solid #4CAF50;'>TOTAL À PAYER</td>" +
                        "<td style='padding: 15px; text-align: right; border-top: 2px solid #4CAF50; color: #4CAF50;'>%.2f DH</td>" +
                        "</tr></table>" +

                        "<!-- Instructions -->" +
                        "<div style='background: #fff3cd; border: 1px solid #ffeaa7; padding: 25px; border-radius: 10px; margin: 30px 0;'>" +
                        "<h3 style='margin-top: 0; color: #856404;'>📋 Instructions importantes :</h3>" +
                        "<ol style='margin: 0; padding-left: 20px;'>" +
                        "<li style='margin-bottom: 10px;'><strong>Rendez-vous à la pharmacie</strong> dès que possible</li>" +
                        "<li style='margin-bottom: 10px;'><strong>Présentez ce code</strong> de réservation : <span style='background: #4CAF50; color: white; padding: 5px 10px; border-radius: 4px; font-family: monospace;'>%s</span></li>" +
                        "<li style='margin-bottom: 10px;'><strong>Préparez le montant</strong> : %.2f DH</li>" +
                        "<li><strong>Apportez votre ordonnance</strong> si nécessaire</li>" +
                        "</ol></div>" +

                        "</div>" +

                        "<!-- Footer -->" +
                        "<div style='background: #f8f9fa; padding: 25px; text-align: center; border-radius: 0 0 15px 15px;'>" +
                        "<p style='margin: 0; color: #666;'>Merci de faire confiance à <strong>PharmaFès</strong></p>" +
                        "<p style='margin: 5px 0 0 0; font-size: 12px; color: #999;'>Email automatique - Ne pas répondre</p>" +
                        "</div>" +

                        "</div></body></html>",

                user.getNom() + " " + user.getPrenom(),
                pharmacie.getName(),
                pharmacie.getName(),
                pharmacie.getEmail(),
                reservation.getId(),
                items.toString(),
                reservation.getTotal(),
                reservation.getId(),
                reservation.getTotal()
        );
    }

    // ============= MÉTHODES UTILITAIRES =============

    private String buildEmailContent(Pharmacie pharmacie, Reservation reservation, User user) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        html.append("<div style='background-color: #4caf50; color: white; padding: 20px; border-radius: 8px 8px 0 0;'>");
        html.append("<h2 style='margin: 0;'>🏥 PharmaFès - Nouvelle Demande de Réservation</h2>");
        html.append("</div>");

        // Contenu principal
        html.append("<div style='background-color: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px;'>");

        html.append("<h3 style='color: #333;'>Bonjour ").append(pharmacie.getName()).append(",</h3>");
        html.append("<p>Vous avez reçu une nouvelle demande de réservation :</p>");

        // Informations client
        html.append("<div style='background-color: white; padding: 15px; border-radius: 6px; margin: 15px 0;'>");
        html.append("<h4 style='color: #4caf50; margin-top: 0;'>👤 Informations Client</h4>");
        html.append("<p><strong>Nom :</strong> ").append(user.getNom()).append(" ").append(user.getPrenom()).append("</p>");
        html.append("<p><strong>Email :</strong> ").append(user.getEmail()).append("</p>");
        html.append("<p><strong>Téléphone :</strong> ").append(user.getTelephone() != null ? user.getTelephone() : "Non renseigné").append("</p>");
        html.append("</div>");

        // Médicaments demandés
        html.append("<div style='background-color: white; padding: 15px; border-radius: 6px; margin: 15px 0;'>");
        html.append("<h4 style='color: #4caf50; margin-top: 0;'>💊 Médicaments Demandés</h4>");

        double totalCommande = 0;
        for (ReservationItem item : reservation.getItems()) {
            double totalItem = item.getQuantite() * item.getPrixUnitaire();
            totalCommande += totalItem;

            html.append("<div style='border: 1px solid #e0e0e0; padding: 10px; margin: 10px 0; border-radius: 4px;'>");
            html.append("<p><strong>Médicament :</strong> ").append(item.getNameMedicament()).append("</p>");
            html.append("<p><strong>Quantité :</strong> ").append(item.getQuantite()).append("</p>");
            html.append("<p><strong>Prix unitaire :</strong> ").append(item.getPrixUnitaire()).append(" DH</p>");
            html.append("<p><strong>Total :</strong> ").append(totalItem).append(" DH</p>");
            html.append("</div>");
        }

        // Total général
        html.append("<div style='background-color: #e8f5e8; padding: 10px; border-radius: 4px; text-align: right;'>");
        html.append("<h4 style='color: #2e7d32; margin: 0;'>Total Commande: ").append(totalCommande).append(" DH</h4>");
        html.append("</div>");
        html.append("</div>");

        // Boutons d'action rapide
        html.append("<div style='background-color: white; padding: 20px; border-radius: 6px; margin: 20px 0; text-align: center;'>");
        html.append("<h4 style='color: #333; margin-top: 0;'>🚀 Action Rapide</h4>");
        html.append("<p style='margin-bottom: 20px;'>Cliquez directement sur l'une des options ci-dessous :</p>");

        String generatedToken = generateToken(reservation.getId(), pharmacie.getId());
        String confirmUrl = baseUrl + "/api/reservations/" + reservation.getId() + "/confirm?token=" + generatedToken + "&pharmacyId=" + pharmacie.getId();
        String rejectUrl = baseUrl + "/api/reservations/" + reservation.getId() + "/reject?token=" + generatedToken + "&pharmacyId=" + pharmacie.getId();

        // Bouton confirmer
        html.append("<a href='").append(confirmUrl).append("' ");
        html.append("style='background-color: #4caf50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 0 10px; display: inline-block; font-weight: bold;'>");
        html.append("✅ CONFIRMER LA RÉSERVATION");
        html.append("</a>");

        // Bouton refuser
        html.append("<a href='").append(rejectUrl).append("' ");
        html.append("style='background-color: #f44336; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 0 10px; display: inline-block; font-weight: bold;'>");
        html.append("❌ REFUSER LA RÉSERVATION");
        html.append("</a>");
        html.append("</div>");

        html.append("</div></div></body></html>");
        return html.toString();
    }

    private String generateToken(String reservationId, String pharmacieId) {
        int hashReservation = reservationId != null ? reservationId.hashCode() : 0;
        int hashPharmacie = pharmacieId != null ? pharmacieId.hashCode() : 0;
        long combinedHash = (long)hashReservation * 31 + hashPharmacie;
        String token = Long.toHexString(combinedHash);
        return token.substring(0, Math.min(16, token.length()));
    }

    private String buildUserReminderEmailContent(User user, Reservation reservation,
                                                 int minutesWaiting, String continueUrl, String expandUrl) {

        double totalReservation = reservation.getItems().stream()
                .mapToDouble(item -> item.getQuantite() * item.getPrixUnitaire())
                .sum();

        StringBuilder itemsList = new StringBuilder();
        for (ReservationItem item : reservation.getItems()) {
            itemsList.append(String.format(
                    "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'>%s</td>" +
                            "<td style='padding: 8px; border-bottom: 1px solid #eee; text-align: center;'>%d</td>" +
                            "<td style='padding: 8px; border-bottom: 1px solid #eee; text-align: right;'>%.2f DH</td></tr>",
                    item.getNameMedicament(), item.getQuantite(), item.getSousTotal()
            ));
        }

        return String.format(
                "<!DOCTYPE html>" +
                        "<html><head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;'>" +

                        "<!-- En-tête -->" +
                        "<div style='background: linear-gradient(135deg, #4CAF50, #45a049); color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;'>" +
                        "<h1 style='margin: 0; font-size: 28px;'>🏥 PharmaFès</h1>" +
                        "<p style='margin: 5px 0 0 0; font-size: 16px; opacity: 0.9;'>Votre réservation en attente</p>" +
                        "</div>" +

                        "<!-- Corps principal -->" +
                        "<div style='background: white; padding: 30px; border: 1px solid #ddd; border-top: none;'>" +

                        "<h2 style='color: #4CAF50; margin-top: 0;'>Bonjour %s,</h2>" +

                        "<div style='background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px; padding: 15px; margin: 20px 0;'>" +
                        "<p style='margin: 0; font-size: 16px;'>" +
                        "⏰ <strong>Votre réservation est en attente depuis %d minutes.</strong><br>" +
                        "Nous recherchons activement une pharmacie pour vous servir." +
                        "</p>" +
                        "</div>" +

                        "<!-- Détails de la réservation -->" +
                        "<div style='background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                        "<h3 style='margin-top: 0; color: #333;'>📋 Détails de votre réservation</h3>" +
                        "<p><strong>Numéro:</strong> %s</p>" +
                        "<p><strong>Date:</strong> %s</p>" +
                        "<p><strong>Rayon de recherche:</strong> %.1f km</p>" +

                        "<table style='width: 100%%; border-collapse: collapse; margin-top: 15px;'>" +
                        "<thead><tr style='background: #4CAF50; color: white;'>" +
                        "<th style='padding: 10px; text-align: left;'>Article</th>" +
                        "<th style='padding: 10px; text-align: center;'>Qté</th>" +
                        "<th style='padding: 10px; text-align: right;'>Sous-total</th>" +
                        "</tr></thead>" +
                        "<tbody>%s</tbody>" +
                        "<tfoot><tr style='background: #f1f1f1; font-weight: bold;'>" +
                        "<td colspan='2' style='padding: 10px;'>Total</td>" +
                        "<td style='padding: 10px; text-align: right;'>%.2f DH</td>" +
                        "</tr></tfoot>" +
                        "</table>" +
                        "</div>" +

                        "<!-- Question et options -->" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "<h3 style='color: #333; margin-bottom: 20px;'>🤔 Que souhaitez-vous faire ?</h3>" +

                        "<!-- Bouton Continuer -->" +
                        "<div style='margin: 15px 0;'>" +
                        "<a href='%s' style='display: inline-block; background: #17a2b8; color: white; padding: 15px 30px; " +
                        "text-decoration: none; border-radius: 8px; font-size: 16px; font-weight: bold; margin: 0 10px;'>" +
                        "⏰ Continuer d'attendre" +
                        "</a>" +
                        "</div>" +

                        "<!-- Bouton Élargir -->" +
                        "<div style='margin: 15px 0;'>" +
                        "<a href='%s' style='display: inline-block; background: #fd7e14; color: white; padding: 15px 30px; " +
                        "text-decoration: none; border-radius: 8px; font-size: 16px; font-weight: bold; margin: 0 10px;'>" +
                        "🔍 Élargir la recherche (+10 km)" +
                        "</a>" +
                        "</div>" +

                        "<p style='font-size: 14px; color: #666; margin-top: 20px;'>" +
                        "Cliquez simplement sur l'une des options ci-dessus pour nous indiquer votre choix." +
                        "</p>" +
                        "</div>" +

                        "</div>" +

                        "<!-- Pied de page -->" +
                        "<div style='background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px; text-align: center; " +
                        "border: 1px solid #ddd; border-top: none;'>" +
                        "<p style='margin: 0; font-size: 12px; color: #666;'>" +
                        "Cet email a été envoyé automatiquement par PharmaFès<br>" +
                        "Si vous avez des questions, contactez-nous à support@pharmafes.ma" +
                        "</p>" +
                        "</div>" +

                        "</body></html>",

                user.getNom() + " " + user.getPrenom(),
                minutesWaiting,
                reservation.getId(),
                reservation.getCreatedAt().toString(),
                reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 5.0,
                itemsList.toString(),
                totalReservation,
                continueUrl,
                expandUrl
        );
    }

    private String generateSecureToken(String reservationId, String action) {
        return Base64.getEncoder().encodeToString(
                (reservationId + ":" + action + ":" + System.currentTimeMillis()).getBytes()
        );
    }

    /**
     * Envoie une notification de recherche élargie aux nouvelles pharmacies
     */
    public void sendExpandedSearchNotification(Pharmacie pharmacie, Reservation reservation, User user,
                                               Double oldRadius, Double newRadius) {
        try {
            if (pharmacie.getEmail() != null) {
                String emailToUse = pharmacie.getEmail().replaceAll("[\\p{C}\\s]+", "");
                pharmacie.setEmail(emailToUse);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(pharmacie.getEmail());
            helper.setSubject("🔍 RECHERCHE ÉLARGIE - Nouvelle demande de réservation - PharmaFès");

            String htmlContent = buildExpandedSearchEmailContent(pharmacie, reservation, user, oldRadius, newRadius);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Email recherche élargie envoyé à: {}", pharmacie.getEmail());

        } catch (Exception e) {
            logger.error("Erreur envoi email recherche élargie à {}: {}", pharmacie.getEmail(), e.getMessage());
            throw new RuntimeException("Échec envoi email recherche élargie", e);
        }
    }

    private String buildExpandedSearchEmailContent(Pharmacie pharmacie, Reservation reservation, User user,
                                                   Double oldRadius, Double newRadius) {
        StringBuilder html = new StringBuilder();

        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header avec indication de recherche élargie
        html.append("<div style='background: linear-gradient(135deg, #2196f3, #1976d2); color: white; padding: 25px; border-radius: 8px 8px 0 0; text-align: center;'>");
        html.append("<h2 style='margin: 0; font-size: 24px;'>🔍 PharmaFès - RECHERCHE ÉLARGIE</h2>");
        html.append("<p style='margin: 8px 0 0 0; font-size: 16px; opacity: 0.9;'>Nouvelle opportunité de réservation dans votre zone</p>");
        html.append("</div>");

        // Contenu principal avec info sur élargissement
        html.append("<div style='background-color: #e3f2fd; padding: 25px; border-radius: 0 0 8px 8px; border: 2px solid #2196f3;'>");

        html.append("<h3 style='color: #333; margin-top: 0;'>Bonjour ").append(pharmacie.getName()).append(",</h3>");
        html.append("<p style='font-size: 16px;'>Cette demande de réservation a été étendue à votre zone géographique :</p>");

        // Info sur l'élargissement
        html.append("<div style='background-color: #fff3e0; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 5px solid #ff9800;'>");
        html.append("<h4 style='color: #f57c00; margin-top: 0; font-size: 18px;'>📍 Extension de Zone de Recherche</h4>");
        html.append("<p style='margin: 8px 0;'><strong>Rayon initial :</strong> ").append(oldRadius).append(" km</p>");
        html.append("<p style='margin: 8px 0;'><strong>Nouveau rayon :</strong> ").append(newRadius).append(" km</p>");
        html.append("<p style='margin: 8px 0; font-style: italic; color: #666;'>➤ Le client n'ayant pas reçu de réponse, sa recherche a été étendue à votre pharmacie.</p>");
        html.append("</div>");

        // Informations client
        html.append("<div style='background-color: white; padding: 20px; border-radius: 8px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");
        html.append("<h4 style='color: #2196f3; margin-top: 0; font-size: 18px;'>👤 Informations Client</h4>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 8px 0; font-weight: bold; width: 30%;'>Nom :</td><td style='padding: 8px 0;'>").append(user.getNom()).append(" ").append(user.getPrenom()).append("</td></tr>");
        html.append("<tr><td style='padding: 8px 0; font-weight: bold;'>Email :</td><td style='padding: 8px 0;'>").append(user.getEmail()).append("</td></tr>");
        html.append("<tr><td style='padding: 8px 0; font-weight: bold;'>Téléphone :</td><td style='padding: 8px 0;'>").append(user.getTelephone() != null ? user.getTelephone() : "Non renseigné").append("</td></tr>");
        html.append("<tr><td style='padding: 8px 0; font-weight: bold;'>Total commande :</td><td style='padding: 8px 0; color: #4caf50; font-weight: bold; font-size: 16px;'>").append(reservation.getTotal()).append(" DH</td></tr>");
        html.append("</table>");
        html.append("</div>");

        // Détails des médicaments (version compacte)
        html.append("<div style='background-color: white; padding: 20px; border-radius: 8px; margin: 20px 0;'>");
        html.append("<h4 style='color: #2196f3; margin-top: 0;'>💊 Médicaments Demandés</h4>");
        html.append("<table style='width: 100%; border-collapse: collapse; border: 1px solid #ddd;'>");
        html.append("<tr style='background: #f5f5f5;'>");
        html.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>Médicament</th>");
        html.append("<th style='padding: 12px; text-align: center; border: 1px solid #ddd;'>Qté</th>");
        html.append("<th style='padding: 12px; text-align: right; border: 1px solid #ddd;'>Total</th>");
        html.append("</tr>");

        for (ReservationItem item : reservation.getItems()) {
            html.append("<tr>");
            html.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(item.getNameMedicament()).append("</td>");
            html.append("<td style='padding: 10px; text-align: center; border: 1px solid #ddd;'>").append(item.getQuantite()).append("</td>");
            html.append("<td style='padding: 10px; text-align: right; border: 1px solid #ddd;'>").append(item.getSousTotal()).append(" DH</td>");
            html.append("</tr>");
        }

        html.append("<tr style='background: #e8f5e9; font-weight: bold;'>");
        html.append("<td colspan='2' style='padding: 12px; border: 1px solid #ddd;'>TOTAL GÉNÉRAL</td>");
        html.append("<td style='padding: 12px; text-align: right; border: 1px solid #ddd; color: #4caf50; font-size: 16px;'>").append(reservation.getTotal()).append(" DH</td>");
        html.append("</tr></table>");
        html.append("</div>");

        // Boutons d'action avec style amélioré
        html.append("<div style='text-align: center; margin: 30px 0; padding: 25px; background: white; border-radius: 8px;'>");
        html.append("<h4 style='color: #333; margin-bottom: 20px; font-size: 20px;'>🚀 Votre Réponse ?</h4>");
        html.append("<p style='margin-bottom: 25px; color: #666; font-size: 16px;'>Cette nouvelle opportunité vous intéresse-t-elle ?</p>");

        // Générer les liens
        String generatedToken = generateToken(reservation.getId(), pharmacie.getId());
        String confirmUrl = baseUrl + "/api/reservations/" + reservation.getId() + "/confirm?token=" + generatedToken + "&pharmacyId=" + pharmacie.getId();
        String rejectUrl = baseUrl + "/api/reservations/" + reservation.getId() + "/reject?token=" + generatedToken + "&pharmacyId=" + pharmacie.getId();

        // Bouton confirmer avec style premium
        html.append("<a href='").append(confirmUrl).append("' ");
        html.append("style='display: inline-block; background: linear-gradient(135deg, #4caf50, #45a049); color: white; padding: 18px 40px; text-decoration: none; border-radius: 8px; margin: 0 15px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 8px rgba(76,175,80,0.3); transition: all 0.3s;'>");
        html.append("✅ CONFIRMER - Je prépare la commande");
        html.append("</a>");

        // Bouton refuser
        html.append("<a href='").append(rejectUrl).append("' ");
        html.append("style='display: inline-block; background: linear-gradient(135deg, #f44336, #d32f2f); color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; margin: 0 15px; font-weight: bold; font-size: 14px; box-shadow: 0 4px 8px rgba(244,67,54,0.3);'>");
        html.append("❌ Pas disponible");
        html.append("</a>");
        html.append("</div>");

        // Note importante
        html.append("<div style='background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 20px; margin: 20px 0; border-radius: 0 8px 8px 0;'>");
        html.append("<h4 style='color: #856404; margin-top: 0;'>⚡ Opportunité Prioritaire</h4>");
        html.append("<p style='margin: 0; color: #856404;'>");
        html.append("<strong>Le client attend activement une réponse.</strong> ");
        html.append("Si vous confirmez, il sera immédiatement notifié et se rendra dans votre pharmacie. ");
        html.append("Réponse rapide appréciée !");
        html.append("</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    public void sendCustomEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("✅ Email personnalisé envoyé à: {}", to);

        } catch (Exception e) {
            logger.error("❌ Erreur envoi email personnalisé à {}: {}", to, e.getMessage());
            throw new RuntimeException("Échec envoi email", e);
        }
    }
}