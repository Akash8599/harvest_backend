package com.banana.harvest.dto.farm;

import com.banana.harvest.entity.enums.InspectionStatus;
import com.banana.harvest.entity.enums.RateStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FarmInspectionResponse {
    private UUID id;
    private UUID farmId;
    private String farmName;
    private String itemName;
    private String farmLocation;
    private UUID vendorId;
    private String vendorName;
    private Integer estimatedBoxes;
    private Integer allocatedBoxes;
    private Integer linersQty;
    private Integer cornersQty;
    private Integer tapeRolls;
    private LocalDate expectedHarvestDate;
    private BigDecimal proposedRate;
    private BigDecimal farmerProposedRate;
    private BigDecimal adminCounterRate;
    private RateStatus rateStatus;
    private String negotiationNotes;
    private String inspectionNotes;
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
    private BigDecimal gpsAccuracy;
    private InspectionStatus status;
    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private List<String> photoUrls;
    private UUID requestId;
    private LocalDateTime createdAt;
}
