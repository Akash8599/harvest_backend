package com.banana.harvest.repository;

import com.banana.harvest.entity.ColdStorageInward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ColdStorageInwardRepository extends JpaRepository<ColdStorageInward, UUID> {
    
    List<ColdStorageInward> findByBatchId(UUID batchId);

    @Query("SELECT COALESCE(SUM(c.totalBoxes), 0) FROM ColdStorageInward c")
    Integer sumTotalInwardBoxes();
}
