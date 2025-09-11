package com.projetPFA.backend_pfa.controllers;

import com.projetPFA.backend_pfa.models.*;
import com.projetPFA.backend_pfa.repositories.PharmacyRepository;
import com.projetPFA.backend_pfa.services.PharmacieScraperService;
import com.projetPFA.backend_pfa.services.PharmacyService;
import com.projetPFA.backend_pfa.services.SimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pharmacies")
@CrossOrigin(origins = "http://localhost:3000")
public class PharmacyController {
    @Autowired
    private PharmacyService pharmacyService;

    @Autowired
    private PharmacyRepository pharmacyRepository;
    @Autowired
    private PharmacieScraperService pharmacieScraperService;

    @Autowired
    private SimulationService simulationService;


    @GetMapping("/All")
    public List<Pharmacie> getAllPharmacies()
    {
        return pharmacyService.getAll();
    }
    @GetMapping("/search")
    public List<Pharmacie> searchPharmacies(@RequestParam String keyword) {
        return pharmacyRepository.findByNameContainingIgnoreCase(keyword);
    }

    //Pharma plus proches
    @GetMapping("/proches")
    public List<Pharmacie> getNearbyPharmacies(
            @RequestParam("lat") double latitude,
            @RequestParam("lon") double longitude,
            @RequestParam(value = "distance", defaultValue = "5") double distanceKm) {
        return pharmacyService.getNearbyPharmacies(latitude, longitude, distanceKm);
    }


    //Extrait toutes les pharmacies de gardes de site puis le compare avec
    //la base de données et puis affiche les pharma de garde
    @GetMapping("/en-garde")
    public List<Pharmacie> getPharmaciesEnGarde() throws IOException {
        pharmacyService.updatePharmaciesEnService();
        return pharmacyService.getPharmaciesEnService();

    }
    @GetMapping("/en-garde-site")
    public List<Pharmacie> getPharmaciesEnGardeSite() throws IOException {

        return pharmacieScraperService.getPharmaciesEnServiceSite();

    }

    //Pharma de garde ,en permanence plus proche
    @GetMapping("/proches-en-garde-en-permanence")
    public List<Pharmacie> getNearbyPharmaciesEnGardeEnPermanence(
            @RequestParam("lat") double latitude,
            @RequestParam("lon") double longitude,
            @RequestParam(value = "distance", defaultValue = "5") double distanceKm) throws IOException {

        // Mettre à jour les pharmacies de garde depuis le site
        pharmacyService.updatePharmaciesEnService();

        // Obtenir toutes les pharmacies proches
        List<Pharmacie> pharmaciesProches = pharmacyService.getNearbyPharmacies(latitude, longitude, distanceKm);

        return pharmaciesProches;
    }
    //Pharma de garde plus proches
    @GetMapping("/proches-en-garde")
    public List<Pharmacie> getNearbyPharmaciesEnGarde(
            @RequestParam("lat") double latitude,
            @RequestParam("lon") double longitude,
            @RequestParam(value = "distance", defaultValue = "5") double distanceKm) throws IOException {

        pharmacyService.updatePharmaciesEnService();

        List<Pharmacie> pharmaciesProches = pharmacyService.getNearbyPharmacies(latitude, longitude, distanceKm);

        // Filtrer celles qui sont en service (garde)
        return pharmaciesProches.stream()
                .filter(Pharmacie::isEnService)
                .collect(Collectors.toList());
    }
    //Affecter les medicaments aux pharma
    @PostMapping("/simuler-stocks")
    public ResponseEntity<String> simulerStocks() {
        simulationService.affecterMedicamentsAuxPharmacies();
        return ResponseEntity.ok("Simulation des stocks effectuée avec succès !");
    }
    //Consulter les medicments disponible dns chaque pharmacie
    @GetMapping("/{id}/medicaments")
    public List<Medicament> getMedicamentsDisponibles(@PathVariable String id) {
        Pharmacie p = pharmacyRepository.findById(id).orElseThrow();
        return p.getMedicamentsDisponibles();
    }


}
