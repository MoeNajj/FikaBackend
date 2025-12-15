package com.bikeparking.backend.model;

import java.io.Serializable;
import java.util.Objects;

public class ParkingSpotId implements Serializable {
    private Integer spotId;
    private Integer locationId;

    // Default Constructor
    public ParkingSpotId() {}

    public ParkingSpotId(Integer spotId, Integer locationId) {
        this.spotId = spotId;
        this.locationId = locationId;
    }

    // HashCode and Equals are required for Composite Keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSpotId that = (ParkingSpotId) o;
        return Objects.equals(spotId, that.spotId) &&
               Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spotId, locationId);
    }
}
