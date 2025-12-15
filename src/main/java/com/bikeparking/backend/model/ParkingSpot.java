package com.bikeparking.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to the BikeParking.ParkingSpots table in MSSQL.
 * Uses @IdClass for the composite primary key (SpotID, LocationID).
 */
@Data // Provided by Lombok: Generates getters, setters, equals, hashCode, and toString
@Entity
@IdClass(ParkingSpotId.class) // Links to the separate composite key class
// CRITICAL FIX: Explicitly specify the exact MSSQL Schema and Table Name to override Hibernate's default lowercasing
@Table(name = "ParkingSpots", schema = "BikeParking") 
public class ParkingSpot {

    // --- COMPOSITE PRIMARY KEY FIELDS ---
    @Id
    @Column(name = "SpotID")
    private Integer spotId;

    @Id
    @Column(name = "LocationID")
    private Integer locationId;

    // --- OTHER FIELDS ---

    @Column(name = "RFIDHash")
    private String rfidHash;

    @Column(name = "UserUUID")
    private UUID userUuid; // Maps to SQL Server UNIQUEIDENTIFIER

    @Column(name = "IsOccupied")
    private Boolean isOccupied; // Maps to SQL Server BIT (0 or 1)

    @Column(name = "LoginTime")
    private LocalDateTime loginTime; // Maps to SQL Server DATETIME2

    @Column(name = "LastUpdate")
    private LocalDateTime lastUpdate; // Maps to SQL Server DATETIME2
    
    // Note: The IsActive column mentioned in the base script is not included here 
    // as it was an incomplete ALTER TABLE statement. If you use it, add:
    // @Column(name = "IsActive")
    // private Boolean isActive;
}
