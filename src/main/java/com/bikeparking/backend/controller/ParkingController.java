package com.bikeparking.backend.controller;

import com.bikeparking.backend.model.ParkingSpot;
import com.bikeparking.backend.model.ParkingSpotId;
import com.bikeparking.backend.repository.ParkingSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    @Autowired
    private ParkingSpotRepository repository;

    // DTO for the incoming JSON request
    public static class ParkRequest {
        public Integer spotId;
        public Integer locationId;
        public String rfidHash;
        public String userUuid;
    }

    @PostMapping("/occupy")
    public ResponseEntity<String> occupySpot(@RequestBody ParkRequest request) {
        // Create the composite key to find the record
        ParkingSpotId id = new ParkingSpotId(request.spotId, request.locationId);

        return repository.findById(id).map(spot -> {
            // Update fields per your requirements
            spot.setIsOccupied(true); // Sets DB BIT to 1
            spot.setRfidHash(request.rfidHash);
            
            // Handle String to UUID conversion
            try {
                spot.setUserUuid(UUID.fromString(request.userUuid));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid UUID format");
            }

            spot.setLoginTime(LocalDateTime.now()); // System Time
            spot.setLastUpdate(LocalDateTime.now()); // Good practice to update this too

            repository.save(spot);
            return ResponseEntity.ok("Parking spot updated successfully.");
        }).orElse(ResponseEntity.badRequest().body("Spot not found for given Location/Spot ID."));
    }
}
