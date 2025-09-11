package com.projetPFA.backend_pfa.controllers;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class TestController {

    @GetMapping("/test")
    public String simpleTest() {
        return "✅ Serveur fonctionne parfaitement !";
    }

    @GetMapping("/api/test-pharmacies")
    public Map<String, Object> testPharmacies() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test endpoint pharmacies OK");
        response.put("timestamp", new Date());
        response.put("working", true);
        return response;
    }

    @GetMapping("/api/pharmacies/test-simple")
    public List<Map<String, Object>> testPharmaciesSimple() {
        List<Map<String, Object>> pharmacies = new ArrayList<>();

        Map<String, Object> pharma1 = new HashMap<>();
        pharma1.put("id", "test1");
        pharma1.put("name", "Pharmacie Test 1");
        pharma1.put("latitude", 34.0181);
        pharma1.put("longitude", -5.0078);

        Map<String, Object> pharma2 = new HashMap<>();
        pharma2.put("id", "test2");
        pharma2.put("name", "Pharmacie Test 2");
        pharma2.put("latitude", 34.0200);
        pharma2.put("longitude", -5.0100);

        pharmacies.add(pharma1);
        pharmacies.add(pharma2);

        return pharmacies;
    }
}