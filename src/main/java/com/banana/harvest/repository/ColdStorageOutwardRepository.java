package com.banana.harvest.repository;

import com.banana.harvest.entity.ColdStorageOutward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ColdStorageOutwardRepository extends JpaRepository<ColdStorageOutward, UUID> {

    @Query("SELECT COALESCE(SUM(c.totalBoxes), 0) FROM ColdStorageOutward c")
    Integer sumTotalOutwardBoxes();
}
