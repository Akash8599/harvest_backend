package com.banana.harvest.repository;

import com.banana.harvest.entity.GatePass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GatePassRepository extends JpaRepository<GatePass, UUID> {

    List<GatePass> findByBatchId(UUID batchId);

    List<GatePass> findByDispatchDateBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("SELECT gp FROM GatePass gp WHERE gp.receivedBoxes IS NULL ORDER BY gp.dispatchDate DESC")
    List<GatePass> findPendingGatePasses();

    @Query("SELECT COALESCE(SUM(gp.receivedBoxes), 0) FROM GatePass gp WHERE gp.batch.id = :batchId AND gp.receivedBoxes IS NOT NULL")
    Integer sumReceivedBoxesByBatch(@Param("batchId") UUID batchId);
}
