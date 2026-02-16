package com.banana.harvest.repository;

import com.banana.harvest.entity.InventoryAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryAllocationRepository extends JpaRepository<InventoryAllocation, UUID> {
    
    List<InventoryAllocation> findByBatchId(UUID batchId);
    
    @Query("SELECT ia FROM InventoryAllocation ia WHERE ia.batch.id = :batchId")
    List<InventoryAllocation> findAllocationsByBatch(@Param("batchId") UUID batchId);
    
    @Query("SELECT COALESCE(SUM(ia.quantity), 0) FROM InventoryAllocation ia WHERE ia.batch.id = :batchId AND ia.item.id = :itemId")
    Integer sumAllocatedByBatchAndItem(@Param("batchId") UUID batchId, @Param("itemId") UUID itemId);
}
