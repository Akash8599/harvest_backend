package com.banana.harvest.repository;

import com.banana.harvest.entity.TransportCost;
import com.banana.harvest.entity.enums.TransportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransportCostRepository extends JpaRepository<TransportCost, UUID> {
    
    List<TransportCost> findByBatchId(UUID batchId);
    
    @Query("SELECT tc FROM TransportCost tc WHERE tc.batch.id = :batchId AND tc.costType = :type")
    List<TransportCost> findByBatchAndType(@Param("batchId") UUID batchId, @Param("type") TransportType type);
    
    @Query("SELECT COALESCE(SUM(tc.totalCost), 0) FROM TransportCost tc WHERE tc.batch.id = :batchId AND tc.costType = :type")
    BigDecimal sumCostByBatchAndType(@Param("batchId") UUID batchId, @Param("type") TransportType type);
    
    @Query("SELECT COALESCE(SUM(tc.totalCost), 0) FROM TransportCost tc WHERE tc.batch.id = :batchId")
    BigDecimal sumTotalCostByBatch(@Param("batchId") UUID batchId);
}
