package com.banana.harvest.dto.farm;

import com.banana.harvest.entity.enums.InspectionRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InspectionRequestResponse {
    private UUID id;
    private UUID farmId;
    private String farmName;
    private String farmLocation;
    private String itemName;
    private UUID vendorId;
    private String vendorName;
    private String notes;
    private InspectionRequestStatus status;
    private LocalDate visitDate;
    private String placeOfVisit;
    private String visitorName;
    private String visitorContact;
    private BigDecimal proposedRate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
