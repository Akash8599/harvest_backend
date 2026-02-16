package com.banana.harvest.dto.farm;

import com.banana.harvest.entity.enums.BatchStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBatchStatusRequest {
    @NotNull(message = "Status is required")
    private BatchStatus status;
}
