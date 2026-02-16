package com.banana.harvest.repository;

import com.banana.harvest.entity.Batch;
import com.banana.harvest.entity.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {
    
    Optional<Batch> findByBatchId(String batchId);
    
    List<Batch> findByStatus(BatchStatus status);
    
    Page<Batch> findByStatus(BatchStatus status, Pageable pageable);
    
    List<Batch> findByVendorId(UUID vendorId);
    
    @Query("SELECT b FROM Batch b WHERE b.vendor.id = :vendorId AND b.status = :status")
    List<Batch> findByVendorAndStatus(@Param("vendorId") UUID vendorId, @Param("status") BatchStatus status);
    
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.status = :status")
    Long countByStatus(@Param("status") BatchStatus status);
    
    @Query("SELECT COUNT(b) FROM Batch b")
    Long countTotalBatches();
    
    @Query("SELECT COALESCE(SUM(b.actualBoxes), 0) FROM Batch b WHERE b.status = 'COMPLETED'")
    Integer sumCompletedBoxes();
    
    @Query("SELECT b FROM Batch b WHERE b.status = 'IN_PROGRESS' AND b.vendor.id = :vendorId")
    List<Batch> findActiveBatchesByVendor(@Param("vendorId") UUID vendorId);
}
