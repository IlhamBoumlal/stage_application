package com.projetPFA.backend_pfa.controllers;

import com.projetPFA.backend_pfa.models.DashboardStatistiquesDTO;
import com.projetPFA.backend_pfa.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/statis")

    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<DashboardStatistiquesDTO> getStatistiques()
    {
        DashboardStatistiquesDTO statistiques= dashboardService.getDashboardStatistiques();
        return ResponseEntity.ok(statistiques);
    }
}
