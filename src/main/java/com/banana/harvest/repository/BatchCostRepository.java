package com.banana.harvest.repository;

import com.banana.harvest.entity.BatchCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchCostRepository extends JpaRepository<BatchCost, UUID> {
    
    Optional<BatchCost> findByBatchId(UUID batchId);
    
    @Query("SELECT bc FROM BatchCost bc WHERE bc.batch.id = :batchId")
    Optional<BatchCost> findByBatch(@Param("batchId") UUID batchId);
    
    @Query("SELECT COALESCE(AVG(bc.finalCostPerBox), 0) FROM BatchCost bc WHERE bc.batch.status = 'COMPLETED'")
    BigDecimal averageCostPerBox();
    
    @Query("SELECT COALESCE(SUM(bc.totalCost), 0) FROM BatchCost bc")
    BigDecimal sumTotalCosts();
}
