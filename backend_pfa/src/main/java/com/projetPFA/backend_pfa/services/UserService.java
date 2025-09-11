package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.User;
import com.projetPFA.backend_pfa.models.UserReservationStats;
import com.projetPFA.backend_pfa.repositories.ReservationRepository;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);


    public User findById(String userId) {
        return userRepository.findById(userId).orElse(null);
    }
    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Méthode pour valider l'unicité de l'email lors de la mise à jour
    public boolean emailExistsForOtherUser(String email, String userId) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        return existingUser.isPresent() && !existingUser.get().getId().equals(userId);
    }

    public User findByPhoneNumber(String phoneNumber) {
        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return null;
            }

            // Nettoyer le numéro de téléphone (enlever le préfixe whatsapp: si présent)
            String cleanedPhone = phoneNumber.replace("whatsapp:", "");

            // Rechercher avec différents formats possibles
            List<User> users = userRepository.findByTelephoneContaining(cleanedPhone);

            if (!users.isEmpty()) {
                return users.get(0); // Retourner le premier utilisateur trouvé
            }

            // Si pas trouvé, essayer avec des variations de format
            // Par exemple, si le numéro entrant est +212612345678
            // Essayer aussi avec 0612345678
            if (cleanedPhone.startsWith("+212")) {
                String localFormat = "0" + cleanedPhone.substring(4);
                users = userRepository.findByTelephoneContaining(localFormat);
                if (!users.isEmpty()) {
                    return users.get(0);
                }
            }

            // Si le numéro commence par 06, 07, etc., essayer le format international
            if (cleanedPhone.startsWith("06") || cleanedPhone.startsWith("07")) {
                String internationalFormat = "+212" + cleanedPhone.substring(1);
                users = userRepository.findByTelephoneContaining(internationalFormat);
                if (!users.isEmpty()) {
                    return users.get(0);
                }
            }

            logger.warn("Aucun utilisateur trouvé pour le numéro: {}", phoneNumber);
            return null;

        } catch (Exception e) {
            logger.error("Erreur recherche utilisateur par téléphone {}: {}", phoneNumber, e.getMessage());
            return null;
        }
    }

    public List<UserReservationStats> getUserReservationCounts() {
        List<User> allUsers = userRepository.findAll();
        Map<String, Long> reservationCountsByUserId = reservationRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        reservation -> reservation.getUserId(),
                        Collectors.counting()
                ));

        return allUsers.stream()
                .map(user -> {
                    long count = reservationCountsByUserId.getOrDefault(user.getId(), 0L);
                    return new UserReservationStats(user.getNom() + " " + user.getPrenom(), (int) count);
                })
                .sorted((s1, s2) -> Integer.compare(s2.getReservationCount(), s1.getReservationCount())) // Tri décroissant
                .collect(Collectors.toList());
    }
}
