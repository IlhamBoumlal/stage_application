package com.projetPFA.backend_pfa.services;

import com.projetPFA.backend_pfa.models.Pharmacie;
import com.projetPFA.backend_pfa.repositories.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PharmacyServiceImpl implements PharmacyService {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private PharmacieScraperService scraperService;

    @Override
    public List<Pharmacie> getAll() {
        return pharmacyRepository.findAll();
    }

    @Override
    public List<Pharmacie> getNearbyPharmacies(double userLat, double userLon, double radiusKm) {
        try {
            // Méthode 1 : Essayer avec la requête géospatiale MongoDB native
            Point location = new Point(userLon, userLat); // MongoDB: [longitude, latitude]
            Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
            return pharmacyRepository.findByLocationNear(location, distance);
        } catch (Exception e) {
            // Méthode 2 : Fallback avec calcul manuel si problème géospatial
            System.err.println("Erreur requête géospatiale, utilisation calcul manuel: " + e.getMessage());
            return getNearbyPharmaciesManual(userLat, userLon, radiusKm);
        }
    }

    // Méthode de fallback avec calcul manuel
    private List<Pharmacie> getNearbyPharmaciesManual(double userLat, double userLon, double radiusKm) {
        List<Pharmacie> allPharmacies = pharmacyRepository.findAll();

        return allPharmacies.stream()
                .filter(pharmacie -> {
                    if (pharmacie.getLatitude() == null || pharmacie.getLongitude() == null) {
                        return false;
                    }
                    double distance = calculateDistance(userLat, userLon,
                            pharmacie.getLatitude(), pharmacie.getLongitude());
                    return distance <= radiusKm;
                })
                .sorted((p1, p2) -> {
                    double dist1 = calculateDistance(userLat, userLon, p1.getLatitude(), p1.getLongitude());
                    double dist2 = calculateDistance(userLat, userLon, p2.getLatitude(), p2.getLongitude());
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());
    }

    // Méthode pour calculer la distance entre deux points GPS (Haversine)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance en km
    }

    // Nettoyage des noms pour la comparaison
    private String normalizeNom(String nom) {
        return nom.toLowerCase()
                .replaceAll("[éèêë]", "e")
                .replaceAll("[àâä]", "a")
                .replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o")
                .replaceAll("[ûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9 ]", "") // supprime ponctuation
                .replaceAll("\\s+", " ") // supprime espaces multiples
                .trim();
    }

    // Mettre à jour le statut des pharmacies de garde
    @Override
    public void updatePharmaciesEnService() throws IOException {
        Set<String> enService = scraperService.getPharmaciesEnService();

        // Normaliser tous les noms scrapés une seule fois
        Set<String> nomsEnServiceNormalises = enService.stream()
                .map(this::normalizeNom)
                .collect(Collectors.toSet());

        List<Pharmacie> toutesPharmacies = pharmacyRepository.findAll();

        for (Pharmacie pharmacie : toutesPharmacies) {
            String nomPharma = normalizeNom(pharmacie.getName());
            boolean isEnService = nomsEnServiceNormalises.contains(nomPharma);
            pharmacie.setEnService(isEnService);
            pharmacyRepository.save(pharmacie);
        }
    }

    @Override
    public List<Pharmacie> getPharmaciesEnService() {
        return pharmacyRepository.findByEnServiceTrue();
    }
    @Override
    public Optional<Pharmacie> findById(String id) {
        return pharmacyRepository.findById(id);
    }
}