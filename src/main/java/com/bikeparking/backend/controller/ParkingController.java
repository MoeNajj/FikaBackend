package com.bikeparking.backend.controller;

import com.bikeparking.backend.dto.ParkingSpotRequest;
import com.bikeparking.backend.dto.ToggleResponse;
import com.bikeparking.backend.model.ParkingSpot;
import com.bikeparking.backend.service.ParkingSpotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingSpotService parkingSpotService;

    // Dependency Injection (Spring automatically provides the service)
    public ParkingController(ParkingSpotService parkingSpotService) {
        this.parkingSpotService = parkingSpotService;
    }

    /**
     * Handles the unified request to either occupy or vacate a parking spot.
     * The logic determines the action based on the spot's current status and the request credentials.
     *
     * @param request The DTO containing SpotID, LocationID, RFIDHash, and UserUUID.
     * @return A ResponseEntity containing the ToggleResponse DTO with clear status message.
     */
    @PostMapping("/toggle") 
    public ResponseEntity<ToggleResponse> toggleSpotStatus(@RequestBody ParkingSpotRequest request) {
        
        // Call the service layer method which contains the logic for parking/vacating
        ToggleResponse response = parkingSpotService.toggleParkingSpot(request);
        
        return ResponseEntity.ok(response);
    }
    
    // Optional: Example of a GET method to check a spot's status
    /*
    @GetMapping("/{locationId}/{spotId}")
    public ResponseEntity<ParkingSpot> getSpotStatus(
            @PathVariable Integer locationId, 
            @PathVariable Integer spotId) {
        // You would need to implement findSpot in your service
        // ParkingSpot spot = parkingSpotService.findSpot(locationId, spotId); 
        // return ResponseEntity.ok(spot);
        
        // Placeholder implementation (requires service method):
        return ResponseEntity.notFound().build();
    }
    */
}
