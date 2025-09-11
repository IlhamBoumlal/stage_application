package com.projetPFA.backend_pfa.repositories;

import com.projetPFA.backend_pfa.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User,String> {
    // Optional:Le résultat peut exister ou pas.
    //findByEmail:Spring Data lit le nom de la méthode et génère la requête correspondante
    Optional<User> findByEmail(String email);
    //vérifier l’unicité avant inscription.
    boolean existsByEmail(String email);


    // Méthodes supplémentaires si nécessaires
    Optional<User> findByTelephone(String telephone);
    boolean existsByTelephone(String telephone);
    @Query("SELECT u FROM User u WHERE u.telephone LIKE %:phone%")
    List<User> findByTelephoneContaining(@Param("phone") String phone);

    /**
     * Trouve un utilisateur par numéro de téléphone exact
     */
}
