package com.bikeparking.backend.service;

import com.bikeparking.backend.dto.ParkingSpotRequest;
import com.bikeparking.backend.dto.ToggleResponse;
import com.bikeparking.backend.dto.ToggleResponse.Action;
import com.bikeparking.backend.model.ParkingSpot;
import com.bikeparking.backend.model.ParkingSpotId;
import com.bikeparking.backend.repository.ParkingSpotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;

    public ParkingSpotService(ParkingSpotRepository parkingSpotRepository) {
        this.parkingSpotRepository = parkingSpotRepository;
    }

    /**
     * Toggles the parking status of a spot: occupies if vacant, vacates if occupied by the same user.
     * Throws 409 Conflict if occupied by a different user.
     *
     * @param request DTO containing the spot keys and user/RFID credentials.
     * @return ToggleResponse DTO indicating the action taken and updated spot details.
     */
    public ToggleResponse toggleParkingSpot(ParkingSpotRequest request) {
        
        ParkingSpotId id = new ParkingSpotId(request.getSpotId(), request.getLocationId());

        // 1. Retrieve the existing spot or throw 404
        ParkingSpot spot = parkingSpotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));

        LocalDateTime now = LocalDateTime.now();
        spot.setLastUpdate(now);
        
        Action actionTaken; // Variable to track the outcome

        // --- TOGGLE LOGIC ---

        // Check if the spot is currently occupied (null check prevents NullPointerException)
        if (spot.getIsOccupied() != null && spot.getIsOccupied()) {
            
            // Case A: Spot is OCCUPIED. Check if the current request is from the owner (to vacate).
            
            // Note: Null checks added for safety against DB returning null for cleared fields
            boolean sameUser = spot.getRfidHash() != null && spot.getRfidHash().equals(request.getRfidHash()) && 
                               spot.getUserUuid() != null && spot.getUserUuid().equals(request.getUserUuid());

            if (sameUser) {
                // VACATE ACTION: Same user/RFID is vacating the spot.
                spot.setIsOccupied(false);
                spot.setRfidHash(null);
                spot.setUserUuid(null);
                spot.setLoginTime(null);
                actionTaken = Action.VACATED; 
                
            } else {
                // CONFLICT: Spot is occupied by someone else.
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Spot is currently occupied by a different user. Cannot occupy/vacate.");
            }

        } else {
            // Case B: Spot is VACANT (IsOccupied is null or false).
            
            // OCCUPY ACTION: Occupy the spot with new data.
            spot.setIsOccupied(true);
            spot.setRfidHash(request.getRfidHash());
            spot.setUserUuid(request.getUserUuid());
            spot.setLoginTime(now);
            actionTaken = Action.PARKED; 
        }

        // 3. Save the updated spot back to the database
        ParkingSpot updatedSpot = parkingSpotRepository.save(spot);

        // 4. Return the new clear response object
        String message = actionTaken == Action.PARKED ? 
                         "Bike successfully parked." : 
                         "Bike successfully vacated. Parking duration recorded.";
        
        return ToggleResponse.builder()
                .statusMessage(message)
                .isOccupied(updatedSpot.getIsOccupied())
                .spotDetails(updatedSpot)
                .build();
    }
}
