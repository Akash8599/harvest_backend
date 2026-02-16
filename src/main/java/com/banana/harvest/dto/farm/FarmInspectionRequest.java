package com.banana.harvest.dto.farm;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FarmInspectionRequest {

    @NotNull(message = "Farm ID is required")
    private String farmId;

    @NotNull(message = "Estimated boxes is required")
    private Integer estimatedBoxes;

    private String inspectionNotes;

    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;

    private BigDecimal gpsAccuracy;

    private String requestId;

    private List<String> photoUrls;
}
