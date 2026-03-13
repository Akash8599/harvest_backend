package com.banana.harvest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DatabaseFixRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Executing database fix for estimated_boxes...");
            jdbcTemplate.execute("ALTER TABLE farm_inspections ALTER COLUMN estimated_boxes DROP NOT NULL");
            log.info("Successfully dropped NOT NULL constraint from farm_inspections.estimated_boxes");
        } catch (Exception e) {
            log.warn("Database fix failed (might already be applied or table not in default schema): {}", e.getMessage());
        }
    }
}
