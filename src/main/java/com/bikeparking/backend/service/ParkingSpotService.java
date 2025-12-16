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
import java.util.List;

@Service
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;

    public ParkingSpotService(ParkingSpotRepository parkingSpotRepository) {
        this.parkingSpotRepository = parkingSpotRepository;
    }

    public List<ParkingSpot> getParkingSpots(Integer locationId, List<Integer> spotIds) {
        
        if (locationId == null) {
            return parkingSpotRepository.findAll();
        } 
        
        if (spotIds == null || spotIds.isEmpty()) {
            return parkingSpotRepository.findByLocationId(locationId);
        } 
        
        return parkingSpotRepository.findByLocationIdAndSpotIdIn(locationId, spotIds);
    }

    public ToggleResponse toggleParkingSpot(ParkingSpotRequest request) {
        
        ParkingSpotId id = new ParkingSpotId(request.getSpotId(), request.getLocationId());

        ParkingSpot spot = parkingSpotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));

        LocalDateTime now = LocalDateTime.now();
        spot.setLastUpdate(now);
        
        Action actionTaken;

        if (spot.getIsOccupied() != null && spot.getIsOccupied()) {
            
            boolean sameUser = spot.getRfidHash() != null && spot.getRfidHash().equals(request.getRfidHash()) && 
                               spot.getUserUuid() != null && spot.getUserUuid().equals(request.getUserUuid());

            if (sameUser) {
                // VACATE ACTION
                spot.setIsOccupied(false);
                spot.setRfidHash(null);
                spot.setUserUuid(null);
                spot.setLoginTime(null);
                actionTaken = Action.VACATED; 
                
            } else {
                // CONFLICT
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Spot is currently occupied by a different user. Cannot occupy/vacate.");
            }

        } else {
            // OCCUPY ACTION
            spot.setIsOccupied(true);
            spot.setRfidHash(request.getRfidHash());
            spot.setUserUuid(request.getUserUuid());
            spot.setLoginTime(now);
            actionTaken = Action.PARKED; 
        }

        ParkingSpot updatedSpot = parkingSpotRepository.save(spot);

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
