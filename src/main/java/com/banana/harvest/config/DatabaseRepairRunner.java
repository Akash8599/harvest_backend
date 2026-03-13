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
                    "ALTER TABLE banana_harvest.batches ADD CONSTRAINT batches_status_check CHECK (status IN ('CREATED', 'IN_PROGRESS', 'HARVEST_IN_PROGRESS', 'HARVEST_COMPLETED', 'DISPATCH_IN_PROGRESS', 'DISPATCH_COMPLETED', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'CANCELLED'))");
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

        // Fix estimated_boxes NOT NULL constraint
        try {
            log.info("Executing database fix for estimated_boxes...");
            jdbcTemplate.execute("ALTER TABLE IF EXISTS banana_harvest.farm_inspections ALTER COLUMN estimated_boxes DROP NOT NULL");
            log.info("Successfully dropped NOT NULL constraint from farm_inspections.estimated_boxes");
        } catch (Exception e) {
            log.warn("Database fix failed for estimated_boxes: {}", e.getMessage());
        }

        // Add FARMER_COUNTERED to RateStatus ENUM
        try {
            log.info("Executing database fix for RateStatus ENUM...");
            // PostgreSQL ALTER TYPE cannot run inside a transaction block easily, but Spring boot wraps runners. 
            // We use standard string execution. Enum values are strictly quoted.
            jdbcTemplate.execute("ALTER TYPE banana_harvest.rate_status ADD VALUE IF NOT EXISTS 'FARMER_COUNTERED'");
            log.info("Successfully added FARMER_COUNTERED to rate_status ENUM");
        } catch (Exception e) {
            log.warn("RateStatus ENUM fix failed (might already be applied or type not found): {}", e.getMessage());
        }

        // Rename vendor_proposed_rate to farmer_proposed_rate
        try {
            log.info("Executing database fix to rename vendor_proposed_rate to farmer_proposed_rate...");
            
            // Check if column exists before renaming to avoid errors on fresh databases
            Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema='banana_harvest' AND table_name='farm_inspections' AND column_name='vendor_proposed_rate'", 
                Integer.class
            );

            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE banana_harvest.farm_inspections RENAME COLUMN vendor_proposed_rate TO farmer_proposed_rate");
                log.info("Successfully renamed vendor_proposed_rate to farmer_proposed_rate");
            } else {
                log.info("Column vendor_proposed_rate does not exist, skipping rename.");
            }
        } catch (Exception e) {
            log.warn("Rename column fix failed: {}", e.getMessage());
        }
    }
}
