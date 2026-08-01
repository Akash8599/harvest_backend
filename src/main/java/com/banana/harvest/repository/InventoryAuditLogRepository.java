package com.banana.harvest.repository;

import com.banana.harvest.entity.InventoryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, UUID> {
    List<InventoryAuditLog> findByItemIdOrderByTimestampDesc(UUID itemId);
}
