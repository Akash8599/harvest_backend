package com.banana.harvest.repository;

import com.banana.harvest.entity.FarmInspection;
import com.banana.harvest.entity.enums.InspectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FarmInspectionRepository extends JpaRepository<FarmInspection, UUID> {
    
    List<FarmInspection> findByStatus(InspectionStatus status);
    
    Page<FarmInspection> findByStatus(InspectionStatus status, Pageable pageable);
    
    List<FarmInspection> findByVendorId(UUID vendorId);
    
    @Query("SELECT fi FROM FarmInspection fi WHERE fi.vendor.id = :vendorId AND fi.status = :status")
    List<FarmInspection> findByVendorAndStatus(@Param("vendorId") UUID vendorId, @Param("status") InspectionStatus status);
    
    @Query("SELECT fi FROM FarmInspection fi WHERE fi.farm.id = :farmId ORDER BY fi.createdAt DESC")
    List<FarmInspection> findByFarmId(@Param("farmId") UUID farmId);
    
    @Query("SELECT COUNT(fi) FROM FarmInspection fi WHERE fi.status = :status")
    Long countByStatus(@Param("status") InspectionStatus status);
    
    @Query("SELECT COUNT(fi) FROM FarmInspection fi WHERE fi.vendor.id = :vendorId AND fi.status IN ('PENDING', 'ASSIGNED', 'REQUESTED', 'IN_PROGRESS')")
    Long countPendingByVendor(@Param("vendorId") UUID vendorId);
}
