package com.banana.harvest.dto.farm;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalRequest {

    private Boolean approved;

    private String rejectionReason;
}
