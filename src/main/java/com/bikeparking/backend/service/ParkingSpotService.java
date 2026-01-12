package com.bikeparking.backend.service;

import com.bikeparking.backend.dto.NfcParkingRequest;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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
        String rfidHash = request.getRfidHash();

        if (spot.getIsOccupied() != null && spot.getIsOccupied()) {
            
            // VACATE ACTION - Match by rfidHash if provided, otherwise match by userUuid only
            boolean sameUser;
            if (rfidHash != null && !rfidHash.isEmpty()) {
                // Match by both rfidHash and userUuid if rfidHash is provided
                sameUser = spot.getRfidHash() != null && spot.getRfidHash().equals(rfidHash) && 
                           spot.getUserUuid() != null && spot.getUserUuid().equals(request.getUserUuid());
            } else {
                // Match by userUuid only if rfidHash is not provided
                sameUser = spot.getUserUuid() != null && spot.getUserUuid().equals(request.getUserUuid());
            }

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
            // OCCUPY ACTION - Generate rfidHash if not provided
            if (rfidHash == null || rfidHash.isEmpty()) {
                String timestamp = now.format(TIMESTAMP_FORMATTER);
                rfidHash = String.format("AUTO-%s-%s", request.getUserUuid(), timestamp);
            }
            
            spot.setIsOccupied(true);
            spot.setRfidHash(rfidHash);
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

    /**
     * Toggle parking spot status for NFC operations.
     * NFC Logic:
     * 1. If isOccupied=true AND isParked=false AND rfidHash matches -> set isParked=true (success)
     * 2. If isOccupied=true AND isParked=true AND rfidHash matches -> set both isParked=false and isOccupied=false (vacate)
     * 
     * @param request NFC parking request containing locationId, spotId, rfidHash, and userUuid
     * @return ToggleResponse with action taken and updated spot details
     */
    public ToggleResponse toggleParkingSpotNfc(NfcParkingRequest request) {
        
        if (request.getRfidHash() == null || request.getRfidHash().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rfidHash is required for NFC operations");
        }
        
        ParkingSpotId id = new ParkingSpotId(request.getSpotId(), request.getLocationId());
        ParkingSpot spot = parkingSpotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));

        LocalDateTime now = LocalDateTime.now();
        spot.setLastUpdate(now);
        
        Action actionTaken;

        // Check if spot is occupied and rfidHash matches
        if (spot.getIsOccupied() != null && spot.getIsOccupied() && 
            spot.getRfidHash() != null && spot.getRfidHash().equals(request.getRfidHash())) {
            
            // rfidHash matches
            if (spot.getIsParked() != null && spot.getIsParked()) {
                // State 2: isOccupied=true AND isParked=true AND rfidHash matches
                // -> set both isParked=false and isOccupied=false (vacate)
                spot.setIsOccupied(false);
                spot.setIsParked(false);
                spot.setRfidHash(null);
                spot.setUserUuid(null);
                spot.setLoginTime(null);
                actionTaken = Action.VACATED;
            } else {
                // State 1: isOccupied=true AND isParked=false AND rfidHash matches
                // -> set isParked=true (success)
                spot.setIsParked(true);
                actionTaken = Action.PARKED; // Still considered PARKED action
            }
        } else {
            // rfidHash doesn't match or spot is not occupied - conflict
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "RFID hash does not match or spot is not occupied. Cannot toggle.");
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
