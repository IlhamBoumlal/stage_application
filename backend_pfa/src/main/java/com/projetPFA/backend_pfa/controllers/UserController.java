package com.projetPFA.backend_pfa.controllers;

import com.projetPFA.backend_pfa.models.UserReservationStats;
import com.projetPFA.backend_pfa.repositories.UserRepository;
import com.projetPFA.backend_pfa.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {


    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @GetMapping("/stats/reservations")
    public ResponseEntity<List<UserReservationStats>> getUserReservationStats() {
        List<UserReservationStats> stats = userService.getUserReservationCounts();
        return ResponseEntity.ok(stats);
    }

}