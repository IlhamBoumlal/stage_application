package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.DashboardStatistiquesDTO;
import com.projetPFA.backend_pfa.repositories.MedicamentRepository;
import com.projetPFA.backend_pfa.repositories.PharmacyRepository;
import com.projetPFA.backend_pfa.repositories.ReservationRepository;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PharmacyRepository pharmacieRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MedicamentRepository medicamentRepository;

    public DashboardStatistiquesDTO getDashboardStatistiques() {
        DashboardStatistiquesDTO stats = new DashboardStatistiquesDTO();

        stats.setTotalUsers(userRepository.count());
        stats.setTotalPharmacies(pharmacieRepository.count());
        stats.setTotalReservations(reservationRepository.count());
        stats.setTotalMedicaments(medicamentRepository.count());
    return stats;}

}
