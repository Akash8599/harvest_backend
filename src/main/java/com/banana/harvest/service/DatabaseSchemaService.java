package com.banana.harvest.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSchemaService {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void updateConstraints() {
        log.info("Initializing DatabaseSchemaService: Checking and updating database constraints...");
        try {
            // Fixed: Refresh the farms_status_check constraint to include new HARVEST_IN_PROGRESS status
            // Hibernate's ddl-auto=update does not automatically update existing CHECK constraints.
            
            log.debug("Refreshing farms_status_check constraint...");
            jdbcTemplate.execute("ALTER TABLE banana_harvest.farms DROP CONSTRAINT IF EXISTS farms_status_check");
            jdbcTemplate.execute("ALTER TABLE banana_harvest.farms ADD CONSTRAINT farms_status_check " +
                    "CHECK (status IN ('ACTIVE', 'INSPECTION_PENDING', 'READY_FOR_HARVEST', 'HARVEST_IN_PROGRESS', 'COMPLETED', 'INSPECTION_REJECTED'))");
            
            log.debug("Refreshing batches_status_check constraint...");
            jdbcTemplate.execute("ALTER TABLE banana_harvest.batches DROP CONSTRAINT IF EXISTS batches_status_check");
            jdbcTemplate.execute("ALTER TABLE banana_harvest.batches ADD CONSTRAINT batches_status_check " +
                    "CHECK (status IN ('CREATED', 'IN_PROGRESS', 'HARVEST_IN_PROGRESS', 'HARVEST_COMPLETED', 'DISPATCH_IN_PROGRESS', 'DISPATCH_COMPLETED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'CANCELLED'))");
            
            log.info("Database constraints updated successfully.");
        } catch (Exception e) {
            log.error("Failed to update database constraints. This might be because the table doesn't exist yet or permissions are restricted: {}", e.getMessage());
        }
    }
}
