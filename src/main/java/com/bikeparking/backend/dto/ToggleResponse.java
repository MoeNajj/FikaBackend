package com.bikeparking.backend.dto;

import com.bikeparking.backend.model.ParkingSpot;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToggleResponse {
    private String statusMessage;
    private Boolean isOccupied;
    private ParkingSpot spotDetails;

    public enum Action {
        PARKED, // Spot was vacant, now occupied
        VACATED // Spot was occupied, now vacant
    }
}
