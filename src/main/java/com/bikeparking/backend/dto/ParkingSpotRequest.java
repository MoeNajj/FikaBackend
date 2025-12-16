package com.bikeparking.backend.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class ParkingSpotRequest {
    private Integer spotId;
    private Integer locationId;
    private String rfidHash;
    private UUID userUuid;
}
