package com.banana.harvest.repository;

import com.banana.harvest.entity.InventoryItem;
import com.banana.harvest.entity.enums.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    
    Optional<InventoryItem> findByItemCode(String itemCode);
    
    boolean existsByItemCode(String itemCode);
    
    List<InventoryItem> findByCategory(InventoryCategory category);
    
    List<InventoryItem> findByIsActiveTrue();
}
