package com.banana.harvest.repository;

import com.banana.harvest.entity.DailyHarvestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailyHarvestReportRepository extends JpaRepository<DailyHarvestReport, UUID> {
    
    List<DailyHarvestReport> findByBatchId(UUID batchId);
    
    @Query("SELECT dhr FROM DailyHarvestReport dhr WHERE dhr.batch.id = :batchId ORDER BY dhr.reportDate DESC")
    List<DailyHarvestReport> findByBatchOrderByDate(@Param("batchId") UUID batchId);
    
    @Query("SELECT COALESCE(SUM(dhr.boxesPacked), 0) FROM DailyHarvestReport dhr WHERE dhr.batch.id = :batchId")
    Integer sumBoxesPackedByBatch(@Param("batchId") UUID batchId);
    
    @Query("SELECT COALESCE(SUM(dhr.boxesWasted), 0) FROM DailyHarvestReport dhr WHERE dhr.batch.id = :batchId")
    Integer sumBoxesWastedByBatch(@Param("batchId") UUID batchId);
    
    @Query("SELECT dhr FROM DailyHarvestReport dhr WHERE dhr.reportDate = :date")
    List<DailyHarvestReport> findByReportDate(@Param("date") LocalDate date);
    
    @Query("SELECT COALESCE(SUM(dhr.boxesPacked), 0) FROM DailyHarvestReport dhr WHERE dhr.reportDate = CURRENT_DATE")
    Integer sumTodayBoxesPacked();
}
