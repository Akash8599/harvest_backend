package com.banana.harvest.repository;

import com.banana.harvest.entity.FarmPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FarmPhotoRepository extends JpaRepository<FarmPhoto, UUID> {
    
    List<FarmPhoto> findByInspectionId(UUID inspectionId);
}
