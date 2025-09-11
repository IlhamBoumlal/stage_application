package com.projetPFA.backend_pfa.repositories;

import com.projetPFA.backend_pfa.models.Medicament;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MedicamentRepository extends MongoRepository<Medicament,String> {
}
