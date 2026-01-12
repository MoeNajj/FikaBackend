package com.bikeparking.backend.dto;

import java.util.UUID;
import lombok.Data;

/**
 * Request DTO for NFC-based parking operations.
 * rfidHash must be provided in the request for matching logic.
 */
@Data
public class NfcParkingRequest {
    private Integer spotId;
    private Integer locationId;
    private String rfidHash;
    private UUID userUuid;
}

