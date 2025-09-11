package com.projetPFA.backend_pfa.controllers;

import com.projetPFA.backend_pfa.models.Medicament;
import com.projetPFA.backend_pfa.models.Pharmacie;
import com.projetPFA.backend_pfa.repositories.MedicamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medicaments")
@CrossOrigin(origins = "http://localhost:3000")
public class MedController {
    @Autowired
    private MedicamentRepository medicamentRepository;

    @GetMapping("/all")
    public List<Medicament> getAllMeds()
    {
        return medicamentRepository.findAll();
    }

}
