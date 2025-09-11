package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.*;
import com.projetPFA.backend_pfa.repositories.*;
import com.projetPFA.backend_pfa.services.reservations.EmailNotificationService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PharmacyService pharmacyService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    public Reservation save(Reservation reservation) {
        // Mettre à jour le updatedAt lors de chaque sauvegarde
        reservation.setUpdatedAt(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    public List<Reservation> findByUserId(String userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Reservation> findById(String id) {
        return reservationRepository.findById(id);
    }

    // Méthode simple pour récupérer une réservation (pour le scheduler)
    public Reservation findByIdSimple(String id) {
        Optional<Reservation> reservation = reservationRepository.findById(id);
        return reservation.orElse(null);
    }

    public Reservation updateStatus(String id, String nouveauStatus) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        reservation.setStatus(nouveauStatus);
        reservation.setUpdatedAt(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    public List<Reservation> getReservationsByUserId(String userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Reservation> findByStatusAndDateReservationBefore(String status, LocalDateTime dateTime) {
        return reservationRepository.findByStatusAndCreatedAtBefore(status, dateTime);
    }

    public List<Reservation> findByStatus(String status) {
        return reservationRepository.findByStatus(status);
    }

    // Nouvelle méthode pour le scheduler (si vous utilisez le pattern reminderSent)
    public List<Reservation> findPendingReservationsForReminder(String status, boolean reminderSent) {
        return reservationRepository.findByStatusAndReminderSent(status, reminderSent);
    }

    // Si vous utilisez timeoutAt, vous pouvez avoir une méthode comme celle-ci:
    public List<Reservation> findReservationsOverdueForReminder(String status, LocalDateTime now, boolean reminderSent) {
        return reservationRepository.findByStatusAndTimeoutAtBeforeAndReminderSent(status, now, reminderSent);
    }

    /**
     * Élargit le rayon de recherche pour une réservation
     */
    public boolean expandSearchRadius(String reservationId) {
        try {
            logger.info("📍 Élargissement du rayon pour réservation {}", reservationId);

            Reservation reservation = findByIdSimple(reservationId);
            if (reservation == null) {
                logger.warn("⚠️ Réservation {} non trouvée", reservationId);
                return false;
            }

            // Calculer le nouveau rayon (+10km comme demandé)
            Double currentRadius = reservation.getSearchRadius() != null ? reservation.getSearchRadius() : 5.0;
            Double newRadius = currentRadius + 10.0;

            // Mettre à jour le statut
            reservation.setStatus("EN_RECHERCHE_ELARGIE");
            reservation.setSearchRadius(newRadius);
            reservation.setExpandedAt(LocalDateTime.now());
            save(reservation);

            // Relancer la recherche avec un rayon élargi
            boolean searchResult = searchPharmaciesInExpandedRadius(reservation, currentRadius, newRadius);

            if (searchResult) {
                logger.info("✅ Recherche élargie lancée pour réservation {}", reservationId);
                return true;
            } else {
                logger.warn("⚠️ Aucune pharmacie trouvée même avec rayon élargi pour {}", reservationId);
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ Erreur élargissement rayon pour {}: {}", reservationId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Recherche des pharmacies dans un rayon élargi et envoie les emails
     */
    private boolean searchPharmaciesInExpandedRadius(Reservation reservation, Double oldRadius, Double newRadius) {
        try {
            // Récupérer l'utilisateur
            User user = userService.findById(reservation.getUserId());
            if (user == null || reservation.getUserLatitude() == null || reservation.getUserLongitude() == null) {
                logger.warn("⚠️ Données utilisateur manquantes pour réservation {}", reservation.getId());
                return false;
            }

            // Rechercher les pharmacies dans le rayon élargi
            List<Pharmacie> expandedPharmacies = pharmacyService.getNearbyPharmacies(
                    reservation.getUserLatitude(),
                    reservation.getUserLongitude(),
                    newRadius
            );

            if (expandedPharmacies.isEmpty()) {
                logger.info("ℹ️ Aucune pharmacie trouvée dans le rayon élargi de {}km", newRadius);
                return false;
            }

            logger.info("📍 {} pharmacies trouvées dans le rayon élargi", expandedPharmacies.size());

            // Envoyer les emails aux nouvelles pharmacies
            int pharmaciesNotified = 0;
            for (Pharmacie pharmacie : expandedPharmacies) {
                if (pharmacie.getEmail() != null && !pharmacie.getEmail().trim().isEmpty()) {
                    try {
                        // Utiliser la méthode d'envoi d'email existante
                        emailNotificationService.sendReservationNotification(pharmacie, reservation, user);
                        pharmaciesNotified++;

                        logger.debug("📧 Email envoyé à pharmacie {} dans rayon élargi", pharmacie.getName());

                    } catch (Exception e) {
                        logger.error("❌ Erreur envoi email à pharmacie {}: {}",
                                pharmacie.getName(), e.getMessage());
                    }
                }
            }

            logger.info("📧 {} pharmacies notifiées avec le rayon élargi", pharmaciesNotified);
            return pharmaciesNotified > 0;

        } catch (Exception e) {
            logger.error("❌ Erreur recherche rayon élargi: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Annule une réservation
     */
    public boolean cancelReservation(String reservationId) {
        try {
            logger.info("❌ Annulation demandée pour réservation {}", reservationId);

            Reservation reservation = findByIdSimple(reservationId);
            if (reservation == null) {
                logger.warn("⚠️ Réservation {} non trouvée pour annulation", reservationId);
                return false;
            }

            // Vérifier que la réservation peut être annulée
            if ("CONFIRMEE".equals(reservation.getStatus()) || "TERMINEE".equals(reservation.getStatus())) {
                logger.warn("⚠️ Impossible d'annuler réservation {} avec statut {}",
                        reservationId, reservation.getStatus());
                return false;
            }

            // Mettre à jour le statut seulement
            reservation.setStatus("ANNULEE");
            reservation.setUpdatedAt(LocalDateTime.now()); // Utiliser updatedAt à la place
            save(reservation);

            logger.info("✅ Réservation {} annulée avec succès", reservationId);
            return true;

        } catch (Exception e) {
            logger.error("❌ Erreur annulation réservation {}: {}", reservationId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remet une réservation en attente (reset le reminder)
     */
    public boolean resetReservationToWaiting(String reservationId) {
        try {
            Reservation reservation = findByIdSimple(reservationId);
            if (reservation == null) {
                logger.warn("⚠️ Réservation {} non trouvée pour reset", reservationId);
                return false;
            }

            reservation.setStatus("EN_ATTENTE");
            reservation.setReminderSent(false);
            reservation.setTimeoutAt(LocalDateTime.now().plusMinutes(15)); // Nouveau timeout
            save(reservation);

            logger.info("🔄 Réservation {} remise en attente", reservationId);
            return true;

        } catch (Exception e) {
            logger.error("❌ Erreur reset réservation {}: {}", reservationId, e.getMessage(), e);
            return false;
        }
    }
    /**
     * Envoie un message de confirmation WhatsApp réel à l'utilisateur
     */
    public boolean sendConfirmationMessage(String phoneNumber, String confirmationMessage) {
        try {
            logger.info("✅ Envoi confirmation à {}", phoneNumber);

            // Message texte simple
            Map<String, Object> textMessage = new HashMap<>();
            textMessage.put("messaging_product", "whatsapp");
            textMessage.put("to", phoneNumber);
            textMessage.put("type", "text");

            Map<String, Object> text = new HashMap<>();
            text.put("body", confirmationMessage);
            textMessage.put("text", text);

            // Envoyer
            String response = sendWhatsAppRequest(textMessage);

            logger.info("✅ Message de confirmation envoyé à {}", phoneNumber);
            return true;

        } catch (Exception e) {
            logger.error("❌ Erreur envoi confirmation à {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }
    private String sendWhatsAppRequest(Map<String, Object> payload) {
        // Implémentez selon votre provider WhatsApp
        // Exemple pour Meta WhatsApp Business API:

        String url = "https://graph.facebook.com/v18.0/PHONE_NUMBER_ID/messages";
        String accessToken = "VOTRE_ACCESS_TOKEN"; // À récupérer de vos propriétés

        // Utilisez votre client HTTP (RestTemplate, OkHttp, etc.)
        // RestTemplate restTemplate = new RestTemplate();
        // HttpHeaders headers = new HttpHeaders();
        // headers.setBearerAuth(accessToken);
        // headers.setContentType(MediaType.APPLICATION_JSON);
        //
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        // ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        //
        // return response.getBody();

        return "success"; // Placeholder
    }


    public void deleteById(String id) {
        reservationRepository.deleteById(id);
    }

    // Nouvelle méthode pour récupérer les statistiques par pharmacie
    public List<ReservationByPharmacyStats> getReservationStatsByPharmacy() {
        // Récupérer toutes les réservations confirmées ou en attente pour les inclure dans les stats
        List<Reservation> reservations = reservationRepository.findByStatusIn(List.of("CONFIRMÉE", "EN_ATTENTE"));

        Map<String, Long> reservationsCountByPharmacy = reservations.stream()
                .filter(r -> r.getConfirmedByPharmacyId() != null) // On ne compte que si confirmée par une pharmacie
                .collect(Collectors.groupingBy(Reservation::getConfirmedByPharmacyId, Collectors.counting()));

        return reservationsCountByPharmacy.entrySet().stream()
                .map(entry -> {
                    String pharmacyId = entry.getKey();
                    Long count = entry.getValue();
                    // Récupère le nom de la pharmacie via son service
                    String pharmacyName = pharmacyService.findById(pharmacyId)
                            .map(p -> p.getName())
                            .orElse("Pharmacie Inconnue"); // Gère le cas où la pharmacie n'est pas trouvée
                    return new ReservationByPharmacyStats(pharmacyName, count.intValue());
                })
                .sorted((s1, s2) -> Integer.compare(s2.getReservations(), s1.getReservations())) // Tri décroissant
                .collect(Collectors.toList());
    }

    public List<Reservation> findByStatusIn(List<String> statuses) {
        return reservationRepository.findByStatusIn(statuses);
    }

    // NOUVELLE MÉTHODE CORRIGÉE : Statistiques de réservations par jour
    public List<ReservationDailyStats> getDailyReservationStats() {
        List<Reservation> allReservations = reservationRepository.findAll();

        // Grouper par LocalDate complet (avec l'année)
        Map<LocalDate, Long> dailyCounts = allReservations.stream()
                .collect(Collectors.groupingBy(
                        reservation -> reservation.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Formatter pour afficher seulement jour/mois
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM");

        return dailyCounts.entrySet().stream()
                .map(entry -> new ReservationDailyStats(
                        entry.getKey().format(displayFormatter),
                        entry.getValue().intValue()
                ))
                // CORRECTION : Trier par la LocalDate complète, pas par la chaîne formatée
                .sorted(Comparator.comparing(stats -> {
                    // Reconstituer la LocalDate à partir de la chaîne dd/MM
                    String[] parts = stats.getDate().split("/");
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    // Utiliser l'année courante
                    return LocalDate.of(LocalDate.now().getYear(), month, day);
                }))
                .collect(Collectors.toList());
    }

}

