package com.bikeparking.backend.repository;

import com.bikeparking.backend.model.ParkingSpot;
import com.bikeparking.backend.model.ParkingSpotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, ParkingSpotId> {

    // Finds parking spots by a specific Location ID.
    List<ParkingSpot> findByLocationId(Integer locationId);

    // Finds parking spots by a specific Location ID and a list of Spot IDs.
    List<ParkingSpot> findByLocationIdAndSpotIdIn(Integer locationId, List<Integer> spotIds);
}
