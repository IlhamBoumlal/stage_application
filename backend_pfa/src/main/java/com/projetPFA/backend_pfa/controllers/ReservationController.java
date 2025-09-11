package com.projetPFA.backend_pfa.controllers;

import com.projetPFA.backend_pfa.models.*;
import com.projetPFA.backend_pfa.services.PharmacyService;
import com.projetPFA.backend_pfa.services.ReservationService;
import com.projetPFA.backend_pfa.services.UserService;
import com.projetPFA.backend_pfa.services.reservations.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);
    @Value("${spring.mail.from}")
    private String fromEmail;
    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PharmacyService pharmacyService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private UserService userService;



    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;
    @PostMapping("/demande-avec-notification")
    public ResponseEntity<?> createReservationWithNotification(@RequestBody ReservationRequest request) {
        try {
            if (request.getUserId() == null || request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Données de réservation invalides : ID utilisateur ou articles manquants."));
            }

            String userId = request.getUserId();
            User user = userService.findById(userId);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Utilisateur non trouvé avec l'ID : " + userId));
            }

            Double userLat = request.getUserLatitude();
            Double userLon = request.getUserLongitude();

            if (userLat == null || userLon == null || userLat < -90 || userLat > 90 || userLon < -180 || userLon > 180) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Coordonnées GPS invalides pour l'utilisateur."));
            }

            Double total = request.getItems().stream()
                    .mapToDouble(item -> item.getQuantite() * item.getPrixUnitaire())
                    .sum();

            List<ReservationItem> reservationItems = request.getItems().stream()
                    .map(itemReq -> {
                        ReservationItem item = new ReservationItem();
                        item.setMedicamentId(itemReq.getMedicamentId());
                        item.setNameMedicament(itemReq.getNameMedicament());
                        item.setQuantite(itemReq.getQuantite());
                        item.setPrixUnitaire(itemReq.getPrixUnitaire());
                        item.setSousTotal(item.getQuantite() * item.getPrixUnitaire());
                        return item;
                    })
                    .toList();

            Reservation reservation = new Reservation(
                    userId,
                    reservationItems,
                    total,
                    LocalDateTime.now(),
                    "EN_ATTENTE",
                    userLat,
                    userLon,
                    request.getSearchRadius()
            );

            reservation.setTimeoutAt(LocalDateTime.now().plusMinutes(3));
            reservation.setReminderSent(false);

            Reservation savedReservation = reservationService.save(reservation);

            Double searchRadius = request.getSearchRadius() != null ? request.getSearchRadius() : 5.0;
            List<Pharmacie> pharmaciesProches = pharmacyService.getNearbyPharmacies(
                    userLat, userLon, searchRadius
            );

            List<Pharmacie> pharmaciesANotifier = pharmaciesProches.stream()
                    .limit(10)
                    .toList();

            int pharmaciesNotified = 0;
            List<String> pharmacyNames = new ArrayList<>();

            for (Pharmacie pharmacie : pharmaciesANotifier) {
                if (pharmacie.getEmail() != null && !pharmacie.getEmail().trim().isEmpty()) {
                    try {
                        String cleanedEmail = pharmacie.getEmail().replaceAll("[\\s\\p{Cntrl}]", "");
                        pharmacie.setEmail(cleanedEmail);

                        emailNotificationService.sendReservationNotification(pharmacie, savedReservation, user);
                        pharmaciesNotified++;
                        pharmacyNames.add(pharmacie.getName());
                    } catch (Exception e) {
                        logger.error("Erreur lors de la notification par email de la pharmacie " + pharmacie.getName() + ": " + e.getMessage());
                    }
                }
            }
            try {
                emailNotificationService.sendReservationCreatedConfirmation(user, savedReservation, pharmaciesNotified);
            } catch (Exception e) {
                logger.error("Erreur envoi confirmation création réservation: " + e.getMessage());
            }

            ReservationResponse response = new ReservationResponse();
            response.setReservationId(savedReservation.getId());
            response.setPharmaciesNotified(pharmaciesNotified);
            response.setPharmacyNames(pharmacyNames);

            if (pharmaciesNotified > 0) {
                response.setMessage("Demande de réservation envoyée avec succès à " + pharmaciesNotified + " pharmacie(s) proche(s).");
                response.setSuggestExpandRadius(false);
            } else {
                response.setMessage("Aucune pharmacie proche n'a pu être notifiée dans le rayon de " + searchRadius + " km.");
                response.setSuggestExpandRadius(true);
            }

            return ResponseEntity.status(201).body(response);

        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la création de réservation: " + e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur interne du serveur. Veuillez réessayer plus tard.", "message", e.getMessage()));
        }
    }


    @GetMapping(value = "/{reservationId}/confirm", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String confirmReservation(
            @PathVariable String reservationId,
            @RequestParam String token,
            @RequestParam String pharmacyId) {

        String messageTitle = "Confirmation de Réservation";
        String messageContent = "";
        String icon = "";
        String additionalInfo = "";

        try {
            Optional<Reservation> reservationOpt = reservationService.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                messageTitle = "Erreur";
                messageContent = "Désolé, la réservation n'a pas été trouvée.";
                icon = "❌";
                return buildHtmlResponseWithDetails(messageTitle, messageContent, icon, additionalInfo);
            }
            Reservation reservation = reservationOpt.get();

            Optional<Pharmacie> pharmacieOpt = pharmacyService.findById(pharmacyId);
            if (pharmacieOpt.isEmpty()) {
                messageTitle = "Erreur";
                messageContent = "Désolé, la pharmacie n'a pas été trouvée.";
                icon = "❌";
                return buildHtmlResponse(messageTitle, messageContent, icon);
            }
            Pharmacie pharmacie = pharmacieOpt.get();

            if (reservation.getConfirmedByPharmacyId() != null) {
                if (reservation.getConfirmedByPharmacyId().equals(pharmacyId)) {
                    messageContent = "Cette réservation a déjà été confirmée par votre pharmacie.";
                    icon = "ℹ️";
                    additionalInfo = "Le client a déjà été notifié précédemment.";
                    return buildHtmlResponse(messageTitle, messageContent, icon);
                } else {
                    messageTitle = "Attention";
                    messageContent = "Cette réservation a déjà été confirmée par une autre pharmacie.";
                    icon = "⚠️";
                    return buildHtmlResponse(messageTitle, messageContent, icon);
                }
            }

            reservation.setStatus("CONFIRMÉE");
            reservation.setConfirmedByPharmacyId(pharmacyId);
            reservationService.save(reservation);

            User user = userService.findById(reservation.getUserId());

            if (user != null) {
                // Envoyer email de confirmation à l'utilisateur
                boolean emailSent = emailNotificationService.sendConfirmationEmail(
                        user,
                        pharmacie,
                        reservation
                );

                if (emailSent) {
                    messageTitle = "✅ Réservation Confirmée !";
                    messageContent = "Parfait ! La réservation a été confirmée avec succès.";
                    additionalInfo = String.format(
                            "<div class='success-info'>" +
                                    "<h3>📧 Notification Email Envoyée</h3>" +
                                    "<p>✅ Le client <strong>%s</strong> a été notifié par email à <strong>%s</strong></p>" +
                                    "<p>Il recevra un message l'informant que sa réservation est prête à être récupérée.</p>" +
                                    "</div>",
                            user.getNom() + " " + user.getPrenom(),
                            user.getEmail()
                    );
                } else {
                    messageTitle = "⚠️ Réservation Confirmée";
                    messageContent = "La réservation a été confirmée, mais il y a eu un problème avec l'envoi de l'email.";
                    icon = "⚠️";
                    additionalInfo = String.format(
                            "<div class='warning-info'>" +
                                    "<h3>📞 Contact Manuel Requis</h3>" +
                                    "<p>Veuillez contacter le client directement :</p>" +
                                    "<ul>" +
                                    "<li><strong>Nom :</strong> %s %s</li>" +
                                    "<li><strong>Email :</strong> %s</li>" +
                                    "<li><strong>Téléphone :</strong> %s</li>" +
                                    "</ul>" +
                                    "</div>",
                            user.getNom(), user.getPrenom(),
                            user.getEmail(),
                            user.getTelephone() != null ? user.getTelephone() : "Non renseigné"
                    );
                }
            }

        } catch (Exception e) {
            logger.error("Erreur lors de la confirmation de réservation: " + e.getMessage());
            messageTitle = "Erreur Système";
            messageContent = "Une erreur inattendue est survenue lors du traitement de votre demande.";
            icon = "❌";
            additionalInfo = "<p>Veuillez réessayer ou contacter le support technique.</p>";
        }

        return buildHtmlResponseWithDetails(messageTitle, messageContent, icon, additionalInfo);
    }

    @GetMapping(value = "/{reservationId}/user-choice", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String handleUserChoice(
            @PathVariable String reservationId,
            @RequestParam String choice,
            @RequestParam String token) {

        String messageTitle = "PharmaFès - Choix confirmé";
        String messageContent = "";
        String icon = "";
        String additionalInfo = "";

        try {
            Optional<Reservation> reservationOpt = reservationService.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                messageTitle = "Erreur";
                messageContent = "Désolé, la réservation n'a pas été trouvée.";
                icon = "❌";
                return buildHtmlResponseWithDetails(messageTitle, messageContent, icon, additionalInfo);
            }

            Reservation reservation = reservationOpt.get();

            if (!"EN_ATTENTE_OPTIONS".equals(reservation.getStatus())) {
                messageTitle = "Action déjà traitée";
                messageContent = "Cette demande a déjà été traitée ou a expiré.";
                icon = "⚠️";
                additionalInfo = "<p>Votre réservation est actuellement dans l'état: <strong>" + reservation.getStatus() + "</strong></p>";
                return buildHtmlResponseWithDetails(messageTitle, messageContent, icon, additionalInfo);
            }

            User user = userService.findById(reservation.getUserId());
            if (user == null) {
                messageTitle = "Erreur";
                messageContent = "Utilisateur non trouvé.";
                icon = "❌";
                return buildHtmlResponseWithDetails(messageTitle, messageContent, icon, additionalInfo);
            }

            switch (choice.toLowerCase()) {
                case "continue":
                    handleContinueWaitingChoice(reservation, user);
                    messageTitle = "✅ D'accord, on continue d'attendre !";
                    messageContent = "Parfait ! Nous continuons à chercher une pharmacie pour vous.";
                    icon = "⏰";
                    additionalInfo = String.format(
                            "<div class='success-info'>" +
                                    "<h3>🔄 Recherche active</h3>" +
                                    "<p><strong>Statut:</strong> Remis en recherche active</p>" +
                                    "<p><strong>Rayon actuel:</strong> %.1f km</p>" +
                                    "<p><strong>Action:</strong> Les pharmacies continuent à recevoir vos demandes</p>" +
                                    "<div class='notification-box'>" +
                                    "<p>📧 <strong>Vous recevrez un email</strong> dès qu'une pharmacie confirmera votre réservation.</p>" +
                                    "<p>⏰ <strong>Temps d'attente estimé:</strong> 2-5 minutes supplémentaires</p>" +
                                    "</div>" +
                                    "</div>",
                            reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 5.0
                    );
                    break;

                case "expand":
                    ExpandSearchResult result = handleExpandSearchChoice(reservation, user);
                    if (result.isSuccess()) {
                        messageTitle = "✅ D'accord, on élargit la recherche !";
                        messageContent = "Excellent ! Votre recherche a été élargie et de nouvelles pharmacies sont contactées.";
                        icon = "🔍";
                        additionalInfo = String.format(
                                "<div class='success-info'>" +
                                        "<h3>📍 Recherche élargie avec succès</h3>" +
                                        "<p><strong>Ancien rayon:</strong> %.1f km</p>" +
                                        "<p><strong>Nouveau rayon:</strong> %.1f km</p>" +
                                        "<p><strong>Nouvelles pharmacies contactées:</strong> %d</p>" +
                                        "<p><strong>Status:</strong> Emails de demande envoyés automatiquement</p>" +
                                        "<div class='notification-box'>" +
                                        "<p>📧 <strong>Processus automatique en cours</strong></p>" +
                                        "<p>• Vos demandes sont envoyées aux nouvelles pharmacies</p>" +
                                        "<p>• Vous serez notifié dès qu'une pharmacie répond</p>" +
                                        "<p>• Le même processus se répète jusqu'à confirmation</p>" +
                                        "</div>" +
                                        "</div>",
                                result.getOldRadius(),
                                result.getNewRadius(),
                                result.getPharmaciesNotified()
                        );
                    } else {
                        messageTitle = "⚠️ Recherche élargie, mais...";
                        messageContent = "D'accord, on élargit la recherche ! Malheureusement, aucune nouvelle pharmacie trouvée dans ce rayon élargi.";
                        icon = "🔍";
                        additionalInfo = String.format(
                                "<div class='warning-info'>" +
                                        "<h3>📍 Élargissement effectué</h3>" +
                                        "<p><strong>Nouveau rayon:</strong> %.1f km</p>" +
                                        "<p><strong>Situation:</strong> Zone géographique couverte entièrement</p>" +
                                        "<p><strong>Prochaines étapes:</strong></p>" +
                                        "<ul>" +
                                        "<li>Nous continuons à surveiller les pharmacies existantes</li>" +
                                        "<li>De nouvelles tentatives seront faites régulièrement</li>" +
                                        "<li>Notre équipe peut vous contacter pour des alternatives</li>" +
                                        "</ul>" +
                                        "<p>📞 <strong>Support direct:</strong> support@pharmafes.ma</p>" +
                                        "</div>",
                                result.getNewRadius()
                        );
                    }
                    break;

                default:
                    messageTitle = "Erreur";
                    messageContent = "Choix invalide.";
                    icon = "❌";
                    additionalInfo = "<p>Les choix valides sont 'continue' ou 'expand'.</p>";
            }

            logger.info("✅ Choix utilisateur traité: {} pour réservation {} (utilisateur: {})",
                    choice, reservationId, user.getNom());

        } catch (Exception e) {
            logger.error("❌ Erreur traitement choix utilisateur: {}", e.getMessage(), e);
            messageTitle = "Erreur Système";
            messageContent = "Une erreur inattendue est survenue lors du traitement de votre choix.";
            icon = "❌";
            additionalInfo = "<p>Notre équipe technique a été notifiée. Veuillez réessayer ou nous contacter directement.</p>";
        }

        return buildHtmlResponseWithDetails(messageTitle, messageContent, icon, additionalInfo);
    }

    private void handleContinueWaitingChoice(Reservation reservation, User user) {
        try {
            reservation.setStatus("EN_ATTENTE");
            reservation.setReminderSent(false);
            reservation.setTimeoutAt(LocalDateTime.now().plusMinutes(5)); // 5 minutes de plus
            reservationService.save(reservation);

            logger.info("🔄 Réservation {} remise en attente normale (choix utilisateur)", reservation.getId());
            sendChoiceConfirmationEmail(user, reservation, "continue");

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la remise en attente: {}", e.getMessage(), e);
            throw e;
        }
    }

    private ExpandSearchResult handleExpandSearchChoice(Reservation reservation, User user) {
        try {
            Double currentRadius = reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 5.0;
            Double newRadius = currentRadius + 10.0;

            logger.info("🔍 Élargissement sur demande utilisateur: {}km -> {}km pour réservation {}",
                    currentRadius, newRadius, reservation.getId());

            // Récupérer toutes les pharmacies dans le nouveau rayon
            List<Pharmacie> toutesPharmaciesNouveauRayon = pharmacyService.getNearbyPharmacies(
                    reservation.getUserLatitude(),
                    reservation.getUserLongitude(),
                    newRadius
            );

            // Récupérer les pharmacies de l'ancien rayon
            List<Pharmacie> pharmaciesAncienRayon = pharmacyService.getNearbyPharmacies(
                    reservation.getUserLatitude(),
                    reservation.getUserLongitude(),
                    currentRadius
            );

            // Identifier les NOUVELLES pharmacies (celles qui n'étaient pas dans l'ancien rayon)
            List<String> anciennesPharmaciesIds = pharmaciesAncienRayon.stream()
                    .map(Pharmacie::getId)
                    .toList();

            List<Pharmacie> nouvellesPharmacies = toutesPharmaciesNouveauRayon.stream()
                    .filter(pharma -> !anciennesPharmaciesIds.contains(pharma.getId()))
                    .toList();

            logger.info("📊 Pharmacies - Ancien rayon: {}, Nouveau rayon: {}, Nouvelles: {}",
                    pharmaciesAncienRayon.size(), toutesPharmaciesNouveauRayon.size(), nouvellesPharmacies.size());

            // Envoyer des emails UNIQUEMENT aux nouvelles pharmacies
            int pharmaciesNotifiees = 0;
            for (Pharmacie pharmacie : nouvellesPharmacies) {
                if (pharmacie.getEmail() != null && !pharmacie.getEmail().trim().isEmpty()) {
                    try {
                        // Envoyer email de demande de réservation avec indication "recherche élargie"
                        emailNotificationService.sendExpandedSearchNotification(pharmacie, reservation, user, currentRadius, newRadius);
                        pharmaciesNotifiees++;
                        logger.info("📧 Email recherche élargie envoyé à: {} ({})", pharmacie.getName(), pharmacie.getEmail());

                        // Pause courte entre envois pour éviter spam
                        Thread.sleep(200);
                    } catch (Exception e) {
                        logger.error("❌ Erreur envoi email recherche élargie à {}: {}", pharmacie.getName(), e.getMessage());
                    }
                }
            }

            // Mettre à jour la réservation
            reservation.setSearchRadius(newRadius);
            reservation.setStatus("EN_ATTENTE"); // Remettre en attente normale
            reservation.setReminderSent(false);
            reservation.setTimeoutAt(LocalDateTime.now().plusMinutes(5)); // Nouveau délai
            reservationService.save(reservation);

            logger.info("✅ Élargissement terminé - {} nouvelles pharmacies contactées", pharmaciesNotifiees);

            // Envoyer email de confirmation à l'utilisateur
            sendChoiceConfirmationEmail(user, reservation, "expand");

            return new ExpandSearchResult(pharmaciesNotifiees > 0, currentRadius, newRadius, pharmaciesNotifiees);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'élargissement: {}", e.getMessage(), e);
            throw e;
        }
    }
    private static class ExpandSearchResult {
        private boolean success;
        private Double oldRadius;
        private Double newRadius;
        private int pharmaciesNotified;

        public ExpandSearchResult(boolean success, Double oldRadius, Double newRadius, int pharmaciesNotified) {
            this.success = success;
            this.oldRadius = oldRadius;
            this.newRadius = newRadius;
            this.pharmaciesNotified = pharmaciesNotified;
        }

        public boolean isSuccess() { return success; }
        public Double getOldRadius() { return oldRadius; }
        public Double getNewRadius() { return newRadius; }
        public int getPharmaciesNotified() { return pharmaciesNotified; }
    }


    @GetMapping(value = "/{reservationId}/reject", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String rejectReservation(
            @PathVariable String reservationId,
            @RequestParam String token,
            @RequestParam String pharmacyId) {

        String messageTitle = "Action de Réservation";
        String messageContent = "";
        String icon = "";

        try {
            Optional<Reservation> reservationOpt = reservationService.findById(reservationId);
            if (reservationOpt.isEmpty()) {
                messageTitle = "Erreur";
                messageContent = "Désolé, la réservation n'a pas été trouvée.";
                icon = "❌";
                return buildHtmlResponse(messageTitle, messageContent, icon);
            }
            Reservation reservation = reservationOpt.get();

            Optional<Pharmacie> pharmacieOpt = pharmacyService.findById(pharmacyId);
            if (pharmacieOpt.isEmpty()) {
                messageTitle = "Erreur";
                messageContent = "Désolé, la pharmacie n'a pas été trouvée.";
                icon = "❌";
                return buildHtmlResponse(messageTitle, messageContent, icon);
            }

            if (reservation.getConfirmedByPharmacyId() != null) {
                messageTitle = "Attention";
                messageContent = "Cette réservation a déjà été confirmée par une autre pharmacie et ne peut pas être refusée.";
                icon = "⚠️";
                return buildHtmlResponse(messageTitle, messageContent, icon);
            }

            reservation.setStatus("REFUSÉE");
            reservationService.save(reservation);

            messageContent = "D'accord, la réservation a été refusée. Le client ne sera pas notifié via WhatsApp pour un refus automatique. Merci.";
            icon = "❌";

        } catch (Exception e) {
            logger.error("Erreur lors du refus de réservation: " + e.getMessage());
            messageTitle = "Erreur Système";
            messageContent = "Une erreur inattendue est survenue lors du traitement de votre demande de refus. Veuillez réessayer ou contacter le support.";
            icon = "❌";
        }

        return buildHtmlResponse(messageTitle, messageContent, icon);
    }

    private String buildHtmlResponseWithDetails(String title, String message, String icon, String additionalInfo) {
        return "<html>" +
                "<head>" +
                "<title>" + title + "</title>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }" +
                ".container { background-color: white; padding: 40px; border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.2); max-width: 600px; margin: 20px; }" +
                "h1 { color: #333; font-size: 2.2em; margin-bottom: 20px; text-align: center; }" +
                "p { color: #555; font-size: 1.2em; line-height: 1.6; text-align: center; margin-bottom: 20px; }" +
                ".icon { font-size: 4em; margin-bottom: 20px; text-align: center; }" +
                ".whatsapp-info { background-color: #e8f5e9; padding: 20px; border-radius: 10px; border-left: 5px solid #4caf50; margin-top: 20px; }" +
                ".warning-info { background-color: #fff3e0; padding: 20px; border-radius: 10px; border-left: 5px solid #ff9800; margin-top: 20px; }" +
                ".contact-info { background-color: #e3f2fd; padding: 20px; border-radius: 10px; border-left: 5px solid #2196f3; margin-top: 20px; }" +
                ".success-info { background-color: #e8f5e9; padding: 20px; border-radius: 10px; border-left: 5px solid #4caf50; margin-top: 20px; }" +
                ".notification-box { background-color: #e3f2fd; padding: 15px; border-radius: 8px; margin-top: 15px; border: 1px solid #2196f3; }" +
                ".whatsapp-info h3, .warning-info h3, .contact-info h3, .success-info h3 { margin-top: 0; color: #333; }" +
                ".success-info p, .warning-info p { text-align: left; margin-bottom: 10px; }" +
                "ul { text-align: left; }" +
                "a { color: #1976d2; text-decoration: none; } a:hover { text-decoration: underline; }" +
                "@media (max-width: 600px) { .container { margin: 10px; padding: 20px; } h1 { font-size: 1.8em; } }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='icon'>" + icon + "</div>" +
                "<h1>" + title + "</h1>" +
                "<p>" + message + "</p>" +
                (additionalInfo != null && !additionalInfo.trim().isEmpty() ? additionalInfo : "") +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildHtmlResponse(String title, String message, String icon) {
        return "<html>" +
                "<head>" +
                "<title>" + title + "</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background-color: #f0f2f5; text-align: center; }" +
                ".container { background-color: white; padding: 40px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); max-width: 500px; }" +
                "h1 { color: #333; font-size: 2em; margin-bottom: 20px; }" +
                "p { color: #555; font-size: 1.1em; line-height: 1.6; }" +
                ".icon { font-size: 3em; margin-bottom: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='icon'>" + icon + "</div>" +
                "<h1>" + title + "</h1>" +
                "<p>" + message + "</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    @GetMapping("/test-gmail")
    public ResponseEntity<String> testGmail() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("votre-email@gmail.com");
            helper.setTo("boumlalilham@gmail.com");
            helper.setSubject("Test Gmail SMTP - PharmaFès");
            helper.setText("<h1>Test réussi avec Gmail!</h1>", true);

            mailSender.send(message);
            return ResponseEntity.ok("Email Gmail envoyé avec succès!");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur Gmail: " + e.getMessage());
        }
    }

    // Ajoutez cette méthode dans votre ReservationController ou EmailNotificationService

    private void sendChoiceConfirmationEmail(User user, Reservation reservation, String choice) {
        try {
            String subject = "PharmaFès - Votre choix a été pris en compte";
            String content;

            if ("continue".equals(choice)) {
                content = String.format(
                        "<!DOCTYPE html>" +
                                "<html><head><meta charset='UTF-8'></head>" +
                                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +

                                "<!-- Header -->" +
                                "<div style='background: linear-gradient(135deg, #17a2b8, #138496); color: white; padding: 25px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                                "<h1 style='margin: 0; font-size: 26px;'>⏰ D'accord, on continue !</h1>" +
                                "<p style='margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;'>Recherche active maintenue</p>" +
                                "</div>" +

                                "<!-- Corps -->" +
                                "<div style='background: white; padding: 30px; border: 1px solid #ddd;'>" +
                                "<h2 style='color: #17a2b8; margin-top: 0;'>Bonjour %s,</h2>" +

                                "<div style='background: #d1ecf1; border-left: 4px solid #17a2b8; padding: 20px; margin: 20px 0;'>" +
                                "<h3 style='margin-top: 0; color: #0c5460;'>✅ Votre choix confirmé</h3>" +
                                "<p style='margin: 0; font-size: 16px;'>" +
                                "<strong>Nous continuons activement la recherche</strong> d'une pharmacie dans le rayon initial de %.1f km." +
                                "</p>" +
                                "</div>" +

                                "<div style='background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                                "<h3 style='margin-top: 0; color: #495057;'>📋 Réservation n° %s</h3>" +
                                "<p style='margin: 5px 0;'><strong>Statut :</strong> Recherche active en cours</p>" +
                                "<p style='margin: 5px 0;'><strong>Zone :</strong> %.1f km autour de votre position</p>" +
                                "<p style='margin: 5px 0;'><strong>Total :</strong> %.2f DH</p>" +
                                "</div>" +

                                "<div style='background: #fff3cd; border: 1px solid #ffeaa7; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                                "<h4 style='margin-top: 0; color: #856404;'>⏱️ Prochaines étapes :</h4>" +
                                "<ul style='margin: 0; color: #856404;'>" +
                                "<li>Les pharmacies continuent à recevoir votre demande</li>" +
                                "<li>Vous serez notifié <strong>immédiatement</strong> dès qu'une pharmacie confirme</li>" +
                                "<li>Temps d'attente estimé : 2-5 minutes supplémentaires</li>" +
                                "</ul>" +
                                "</div>" +

                                "<p style='text-align: center; margin-top: 30px; color: #666;'>" +
                                "Merci de votre patience. Nous mettons tout en œuvre pour vous trouver une pharmacie rapidement." +
                                "</p>" +

                                "</div>" +

                                "<!-- Footer -->" +
                                "<div style='background: #f8f9fa; padding: 20px; text-align: center; border-radius: 0 0 10px 10px;'>" +
                                "<p style='margin: 0; font-size: 12px; color: #666;'>" +
                                "Email automatique PharmaFès - Ne pas répondre<br>" +
                                "Support : support@pharmafes.ma" +
                                "</p>" +
                                "</div>" +

                                "</div></body></html>",

                        user.getNom() + " " + user.getPrenom(),
                        reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 5.0,
                        reservation.getId(),
                        reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 5.0,
                        reservation.getTotal()
                );

            } else {  // choice = "expand"
                content = String.format(
                        "<!DOCTYPE html>" +
                                "<html><head><meta charset='UTF-8'></head>" +
                                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +

                                "<!-- Header -->" +
                                "<div style='background: linear-gradient(135deg, #fd7e14, #e76c0c); color: white; padding: 25px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                                "<h1 style='margin: 0; font-size: 26px;'>🔍 D'accord, on élargit la recherche !</h1>" +
                                "<p style='margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;'>Zone de recherche étendue + nouvelles pharmacies contactées</p>" +
                                "</div>" +

                                "<!-- Corps -->" +
                                "<div style='background: white; padding: 30px; border: 1px solid #ddd;'>" +
                                "<h2 style='color: #fd7e14; margin-top: 0;'>Bonjour %s,</h2>" +

                                "<div style='background: #fff3cd; border-left: 4px solid #fd7e14; padding: 20px; margin: 20px 0;'>" +
                                "<h3 style='margin-top: 0; color: #856404;'>✅ Élargissement automatique effectué</h3>" +
                                "<p style='margin: 0; font-size: 16px;'>" +
                                "<strong>Votre zone de recherche a été élargie</strong> et de nouvelles pharmacies ont été automatiquement contactées." +
                                "</p>" +
                                "</div>" +

                                "<div style='background: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                                "<h3 style='margin-top: 0; color: #1976d2;'>📍 Détails de l'élargissement</h3>" +
                                "<table style='width: 100%%; border-collapse: collapse;'>" +
                                "<tr><td style='padding: 8px 0; font-weight: bold; width: 40%%;'>Ancien rayon :</td><td style='padding: 8px 0;'>%.1f km</td></tr>" +
                                "<tr><td style='padding: 8px 0; font-weight: bold;'>Nouveau rayon :</td><td style='padding: 8px 0; color: #fd7e14; font-weight: bold;'>%.1f km</td></tr>" +
                                "<tr><td style='padding: 8px 0; font-weight: bold;'>Nouvelles pharmacies :</td><td style='padding: 8px 0; color: #4caf50; font-weight: bold;'>%d contactées automatiquement</td></tr>" +
                                "</table>" +
                                "</div>" +

                                "<div style='background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                                "<h3 style='margin-top: 0; color: #495057;'>📋 Réservation n° %s</h3>" +
                                "<p style='margin: 5px 0;'><strong>Statut :</strong> Recherche élargie active</p>" +
                                "<p style='margin: 5px 0;'><strong>Zone étendue :</strong> %.1f km autour de votre position</p>" +
                                "<p style='margin: 5px 0;'><strong>Total :</strong> %.2f DH</p>" +
                                "</div>" +

                                "<div style='background: #d4edda; border: 1px solid #c3e6cb; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                                "<h4 style='margin-top: 0; color: #155724;'>🔄 Processus automatique en cours :</h4>" +
                                "<ul style='margin: 0; color: #155724;'>" +
                                "<li><strong>Emails envoyés</strong> automatiquement aux nouvelles pharmacies</li>" +
                                "<li><strong>Notification immédiate</strong> dès qu'une pharmacie répond</li>" +
                                "<li><strong>Processus répétitif</strong> : si besoin, nouvelle extension possible</li>" +
                                "<li><strong>Suivi continu</strong> jusqu'à confirmation d'une pharmacie</li>" +
                                "</ul>" +
                                "</div>" +

                                "<p style='text-align: center; margin-top: 30px; color: #666; font-size: 16px;'>" +
                                "<strong>Merci de votre patience !</strong><br>" +
                                "Le système recherche maintenant dans un périmètre plus large." +
                                "</p>" +

                                "</div>" +

                                "<!-- Footer -->" +
                                "<div style='background: #f8f9fa; padding: 20px; text-align: center; border-radius: 0 0 10px 10px;'>" +
                                "<p style='margin: 0; font-size: 12px; color: #666;'>" +
                                "Email automatique PharmaFès - Système de recherche élargie<br>" +
                                "Support : support@pharmafes.ma" +
                                "</p>" +
                                "</div>" +

                                "</div></body></html>",

                        user.getNom() + " " + user.getPrenom(),
                        reservation.getSearchRadius() != null ? reservation.getSearchRadius() - 10.0 : 5.0,  // Ancien rayon
                        reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 15.0,          // Nouveau rayon
                        getPharmaciesContactedCount(reservation), // Nombre de pharmacies contactées (vous devrez implémenter cette méthode)
                        reservation.getId(),
                        reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 15.0,
                        reservation.getTotal()
                );
            }

            // Créer et envoyer l'email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(content, true);
            helper.setFrom(fromEmail);

            mailSender.send(message);

            logger.info("📧 Email de confirmation de choix '{}' envoyé à: {}", choice, user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Erreur envoi email de confirmation de choix: {}", e.getMessage());
        }
    }

    // Méthode utilitaire pour compter les pharmacies contactées
    private int getPharmaciesContactedCount(Reservation reservation) {
        try {
            Double currentRadius = reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 15.0;
            Double oldRadius = currentRadius - 10.0;

            // Récupérer les nouvelles pharmacies dans le rayon élargi
            List<Pharmacie> newPharmacies = pharmacyService.getNearbyPharmacies(
                    reservation.getUserLatitude(),
                    reservation.getUserLongitude(),
                    currentRadius
            );

            List<Pharmacie> oldPharmacies = pharmacyService.getNearbyPharmacies(
                    reservation.getUserLatitude(),
                    reservation.getUserLongitude(),
                    oldRadius > 0 ? oldRadius : 5.0
            );

            return Math.max(0, newPharmacies.size() - oldPharmacies.size());

        } catch (Exception e) {
            logger.error("Erreur calcul pharmacies contactées: {}", e.getMessage());
            return 0; // Valeur par défaut en cas d'erreur
        }
    }
    @GetMapping("/stats/by-pharmacy")
    public ResponseEntity<List<ReservationByPharmacyStats>> getReservationStatsByPharmacy() {
        logger.info("Requête reçue pour les stats de réservation par pharmacie.");
        List<ReservationByPharmacyStats> stats = reservationService.getReservationStatsByPharmacy();
        logger.info("Renvoi de {} entrées de stats de réservation par pharmacie.", stats.size());
        return ResponseEntity.ok(stats);
    }


    // ... (Tes autres méthodes, comme createReservationWithNotification, confirmReservation, etc.) ...
// NOUVEL ENDPOINT : Statistiques de réservations par jour
    @GetMapping("/stats/daily")
    public ResponseEntity<List<ReservationDailyStats>> getDailyReservationStats() {
        logger.info("Requête reçue pour les statistiques de réservations journalières.");
        List<ReservationDailyStats> stats = reservationService.getDailyReservationStats();
        logger.info("Renvoi de {} entrées de statistiques journalières.", stats.size());
        return ResponseEntity.ok(stats);
    }

    // Ajoutez cette méthode dans votre ReservationController

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reservation>> getReservationsByUserId(@PathVariable String userId) {
        try {
            logger.info("Récupération des réservations pour l'utilisateur: {}", userId);

            // Vérifier que l'utilisateur existe
            User user = userService.findById(userId);
            if (user == null) {
                logger.warn("Utilisateur non trouvé: {}", userId);
                return ResponseEntity.notFound().build();
            }

            // Récupérer les réservations de l'utilisateur
            List<Reservation> reservations = reservationService.findByUserId(userId);

            logger.info("Trouvé {} réservations pour l'utilisateur {}", reservations.size(), userId);
            return ResponseEntity.ok(reservations);

        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des réservations pour l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

}