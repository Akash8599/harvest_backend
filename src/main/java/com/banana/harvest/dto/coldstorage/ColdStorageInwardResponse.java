package com.banana.harvest.dto.coldstorage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ColdStorageInwardResponse {
    private UUID id;
    private UUID batchId;
    private String batchIdCode;
    private String farmName;
    private String coldStorageName;
    private LocalDate inwardDate;
    private Integer kg13Boxes;
    private Integer kg13_5Boxes;
    private Integer kg7Boxes;
    private Integer kg16Boxes;
    private Integer totalBoxes;
    private String remarks;
    private String createdByName;
    private LocalDateTime createdAt;
}
