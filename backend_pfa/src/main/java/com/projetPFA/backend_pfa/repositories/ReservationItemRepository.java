package com.projetPFA.backend_pfa.repositories;

import com.projetPFA.backend_pfa.models.ReservationItem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReservationItemRepository extends MongoRepository<ReservationItem,String>
{
}
