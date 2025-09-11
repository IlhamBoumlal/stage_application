package com.projetPFA.backend_pfa.repositories;

import com.projetPFA.backend_pfa.models.Pharmacie;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PharmacyRepository extends MongoRepository<Pharmacie,String> {

    List<Pharmacie> findByLocationNear(Point location, Distance distance);
    //généré automatiquement par Spring
    List<Pharmacie> findByEnServiceTrue();
    List<Pharmacie> findByNameContainingIgnoreCase(String keyword);
}
