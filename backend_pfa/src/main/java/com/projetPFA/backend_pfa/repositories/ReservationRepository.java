package com.projetPFA.backend_pfa.repositories;

import com.projetPFA.backend_pfa.models.Reservation;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {

    List<Reservation> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Reservation> findByStatusAndDateReservationBefore(String status, LocalDateTime dateTime);
    List<Reservation> findByStatus(String status);


    List<Reservation> findByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);

    List<Reservation> findByCreatedAtBetween(LocalDateTime dateDebut, LocalDateTime dateFin);

    // Requêtes géospatiales pour les réservations dans une zone
    @Query("{ 'userLatitude': { $gte: ?0, $lte: ?1 }, 'userLongitude': { $gte: ?2, $lte: ?3 } }")
    List<Reservation> findReservationsInArea(Double latMin, Double latMax, Double lonMin, Double lonMax);
    // Nouvelle méthode pour le scheduler
    List<Reservation> findByStatusAndReminderSent(String status, boolean reminderSent);

    // Si vous voulez utiliser timeoutAt :
    List<Reservation> findByStatusAndTimeoutAtBeforeAndReminderSent(String status, LocalDateTime now, boolean reminderSent);
    List<Reservation> findByStatusIn(List<String> statuses);

}