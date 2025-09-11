package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.Pharmacie;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface PharmacyService {
    List<Pharmacie> getAll();
    List<Pharmacie> getNearbyPharmacies(double latitude, double longitude, double distanceKm);
    public void updatePharmaciesEnService() throws IOException;
    public List<Pharmacie> getPharmaciesEnService();
    Optional<Pharmacie> findById(String id);
}
