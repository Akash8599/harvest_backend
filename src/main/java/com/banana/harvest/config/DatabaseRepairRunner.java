package com.banana.harvest.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseRepairRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for batches with null version...");

        int updated = jdbcTemplate.update("UPDATE banana_harvest.batches SET version = 0 WHERE version IS NULL");
        if (updated > 0) {
            log.info("Fixed null version for {} batches.", updated);
        } else {
            log.info("No batches found with null version.");
        }

        // Fix batches_status_check constraint
        log.info("Updating batches_status_check constraint...");
        try {
            jdbcTemplate.execute("ALTER TABLE banana_harvest.batches DROP CONSTRAINT IF EXISTS batches_status_check");
            jdbcTemplate.execute(
                    "ALTER TABLE banana_harvest.batches ADD CONSTRAINT batches_status_check CHECK (status IN ('CREATED', 'IN_PROGRESS', 'HARVEST_IN_PROGRESS', 'HARVEST_COMPLETED', 'DISPATCH_IN_PROGRESS', 'DISPATCH_COMPLETED', 'COMPLETED', 'CANCELLED'))");
            log.info("Successfully updated batches_status_check constraint.");
        } catch (Exception e) {
            log.error("Failed to update status constraint: {}", e.getMessage());
        }
        

        // Initialize Farm status
        // log.info("Checking for farms with null status...");
        try {
            int farmsUpdated = jdbcTemplate.update("UPDATE banana_harvest.farms SET status = 'ACTIVE' WHERE status IS NULL");
            if (farmsUpdated > 0) {
                log.info("Initialized status for {} farms.", farmsUpdated);
            }
        } catch (Exception e) {
            log.error("Failed to update farm status: {}", e.getMessage());
        }
    }}
