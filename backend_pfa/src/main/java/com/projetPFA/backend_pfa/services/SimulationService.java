package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.Medicament;
import com.projetPFA.backend_pfa.models.Pharmacie;
import com.projetPFA.backend_pfa.repositories.MedicamentRepository;
import com.projetPFA.backend_pfa.repositories.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimulationService {
    @Autowired
    private PharmacyRepository pharmacyRepository;
    @Autowired
    private MedicamentRepository medicamentRepository;

    public void affecterMedicamentsAuxPharmacies() {
        List<Pharmacie> pharmacies = pharmacyRepository.findAll();
        List<Medicament> medicaments = medicamentRepository.findAll();

        Random random = new Random();

        for (Pharmacie pharmacie : pharmacies) {
            // Étape 1 : Récupérer les médicaments déjà disponibles
            Set<Medicament> medicamentsAffectes = new HashSet<>(pharmacie.getMedicamentsDisponibles());

            // Étape 2 : Ajouter entre 20 et 50 nouveaux médicaments aléatoires
            int nombreAAjouter = random.nextInt(30) + 20;

            for (int i = 0; i < nombreAAjouter; i++) {
                Medicament medicament = medicaments.get(random.nextInt(medicaments.size()));
                medicamentsAffectes.add(medicament); // Set évite les doublons
            }

            // Étape 3 : Mettre à jour la liste avec les nouveaux médicaments
            pharmacie.setMedicamentsDisponibles(new ArrayList<>(medicamentsAffectes));
            pharmacyRepository.save(pharmacie);
        }
    }

}
