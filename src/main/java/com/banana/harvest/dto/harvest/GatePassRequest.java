package com.banana.harvest.dto.harvest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GatePassRequest {

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotBlank(message = "Truck number is required")
    private String truckNumber;

    @NotBlank(message = "Driver name is required")
    private String driverName;

    private String driverPhone;

    @NotNull(message = "Total boxes is required")
    private Integer totalBoxes;

    @NotNull(message = "Dispatch date is required")
    private LocalDate dispatchDate;

    private String notes;
}
