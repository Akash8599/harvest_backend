package com.banana.harvest.dto.farm;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FarmResponse {
    private UUID id;
    private String farmerName;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String contactNumber;
    private BigDecimal totalArea;
    private String areaUnit;
    private String produceType;
    private UUID createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private com.banana.harvest.entity.enums.FarmStatus status;
}
