package com.banana.harvest.repository;

import com.banana.harvest.entity.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, UUID> {
    
    Optional<InventoryStock> findByItemId(UUID itemId);
    
    @Query("SELECT COALESCE(SUM(s.availableQuantity), 0) FROM InventoryStock s")
    Integer sumAvailableQuantity();
    
    @Query("SELECT COALESCE(SUM(s.reservedQuantity), 0) FROM InventoryStock s")
    Integer sumReservedQuantity();
    
    @Query("SELECT s FROM InventoryStock s WHERE s.item.id = :itemId")
    Optional<InventoryStock> findByItem(@Param("itemId") UUID itemId);
}
