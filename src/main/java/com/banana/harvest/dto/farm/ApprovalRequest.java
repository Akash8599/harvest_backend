package com.banana.harvest.dto.farm;

import com.banana.harvest.entity.enums.InspectionStatus;
import com.banana.harvest.entity.enums.RateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ApprovalRequest {

    private Boolean approved;

    private String rejectionReason;

    private String approvalNotes;

    private LocalDate expectedHarvestDate;

    private Integer allocatedBoxes;

    private UUID boxItemId;

    private Integer linersQty;

    private Integer cornersQty;

    private Integer tapeRolls;

    private InspectionStatus status;

    private RateStatus rateStatus;

    private BigDecimal proposedRate;

    private BigDecimal adminCounterRate;

    private BigDecimal farmerProposedRate;

    private String negotiationNotes;
}
