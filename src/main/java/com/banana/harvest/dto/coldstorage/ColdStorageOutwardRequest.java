package com.banana.harvest.dto.coldstorage;

import com.banana.harvest.entity.enums.Destination;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ColdStorageOutwardRequest {

    @NotBlank(message = "Container Number is required")
    private String containerNumber;

    @NotNull(message = "Destination is required")
    private Destination destination;

    @NotNull(message = "Dispatch Date is required")
    private LocalDate dispatchDate;

    private Integer kg13Boxes;
    private Integer kg13_5Boxes;
    private Integer kg7Boxes;
    private Integer kg16Boxes;

    private String remarks;
}
