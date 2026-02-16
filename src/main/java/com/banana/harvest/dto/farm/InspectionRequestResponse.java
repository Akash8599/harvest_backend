package com.banana.harvest.dto.farm;

import com.banana.harvest.entity.enums.InspectionRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InspectionRequestResponse {
    private UUID id;
    private UUID farmId;
    private String farmName;
    private UUID vendorId;
    private String vendorName;
    private String notes;
    private InspectionRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
