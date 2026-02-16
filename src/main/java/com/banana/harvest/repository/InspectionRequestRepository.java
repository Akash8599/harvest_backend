package com.banana.harvest.repository;

import com.banana.harvest.entity.InspectionRequest;
import com.banana.harvest.entity.enums.InspectionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InspectionRequestRepository extends JpaRepository<InspectionRequest, UUID> {
    
    List<InspectionRequest> findByVendorIdAndStatus(UUID vendorId, InspectionRequestStatus status);
    
    List<InspectionRequest> findByVendorId(UUID vendorId);
    
    List<InspectionRequest> findByStatus(InspectionRequestStatus status);
}
