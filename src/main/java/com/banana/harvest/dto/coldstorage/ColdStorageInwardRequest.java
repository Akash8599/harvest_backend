package com.banana.harvest.dto.coldstorage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ColdStorageInwardRequest {

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotBlank(message = "Cold Storage Name is required")
    private String coldStorageName;

    @NotNull(message = "Inward Date is required")
    private LocalDate inwardDate;

    private Integer kg13Boxes;
    private Integer kg13_5Boxes;
    private Integer kg7Boxes;
    private Integer kg16Boxes;

    private String remarks;
}
