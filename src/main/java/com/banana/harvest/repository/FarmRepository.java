package com.banana.harvest.repository;

import com.banana.harvest.entity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FarmRepository extends JpaRepository<Farm, UUID> {
    
    Page<Farm> findByFarmerNameContainingIgnoreCase(String farmerName, Pageable pageable);
    
    @Query("SELECT f FROM Farm f WHERE LOWER(f.location) LIKE LOWER(CONCAT('%', :location, '%'))")
    List<Farm> findByLocationContaining(@Param("location") String location);
    
    @Query("SELECT COUNT(f) FROM Farm f")
    Long countTotalFarms();
    
    @Query("SELECT f FROM Farm f WHERE f.createdBy.id = :userId")
    List<Farm> findByCreatedBy(@Param("userId") UUID userId);
}
