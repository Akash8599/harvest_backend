package com.banana.harvest.dto.farm;

import com.banana.harvest.entity.enums.BatchStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BatchResponse {
    private UUID id;
    private String batchId;
    private UUID inspectionId;
    private UUID farmId;
    private String farmName;
    private String farmLocation;
    private String produceType;
    private UUID vendorId;
    private String vendorName;
    private BatchStatus status;
    private Integer estimatedBoxes;
    private Integer allocatedBoxes;
    private Integer harvestedBoxes;
    private Integer remainingBoxes;
    private Integer actualBoxes;
    private Integer dispatchedBoxes;
    private Integer gatePassRemaining;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
