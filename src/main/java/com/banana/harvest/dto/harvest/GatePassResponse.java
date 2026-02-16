package com.banana.harvest.dto.harvest;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GatePassResponse {
    private UUID id;
    private UUID batchId;
    private String batchIdCode;
    private String gatePassNo;
    private String truckNumber;
    private String driverName;
    private String driverPhone;
    private Integer totalBoxes;
    private LocalDateTime dispatchDate;
    private Integer receivedBoxes;
    private LocalDateTime receivedAt;
    private String receivedBy;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
}
