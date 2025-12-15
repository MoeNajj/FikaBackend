package com.bikeparking.backend.repository;

import com.bikeparking.backend.model.ParkingSpot;
import com.bikeparking.backend.model.ParkingSpotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Note: The second generic type is ParkingSpotId, not Long
@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, ParkingSpotId> {
}
