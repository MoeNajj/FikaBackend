package com.bikeparking.backend.controller;

import com.bikeparking.backend.dto.ParkingSpotRequest;
import com.bikeparking.backend.dto.ToggleResponse;
import com.bikeparking.backend.model.ParkingSpot;
import com.bikeparking.backend.service.ParkingSpotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingSpotService parkingSpotService;

    public ParkingController(ParkingSpotService parkingSpotService) {
        this.parkingSpotService = parkingSpotService;
    }

    @PostMapping("/toggle") 
    public ResponseEntity<ToggleResponse> toggleSpotStatus(@RequestBody ParkingSpotRequest request) {
        
        ToggleResponse response = parkingSpotService.toggleParkingSpot(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    public ResponseEntity<List<ParkingSpot>> getSpotStatus(
            @RequestParam(required = false) Integer locationId,
            @RequestParam(required = false) List<Integer> spotIds) {
        
        List<ParkingSpot> spots = parkingSpotService.getParkingSpots(locationId, spotIds);
        
        if (spots.isEmpty() && (locationId != null || (spotIds != null && !spotIds.isEmpty()))) {
             return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(spots);
    }
}
