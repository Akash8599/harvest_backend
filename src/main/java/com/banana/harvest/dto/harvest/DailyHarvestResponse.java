package com.banana.harvest.dto.harvest;

import com.banana.harvest.entity.enums.PackingWeightType;
import com.banana.harvest.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DailyHarvestResponse {
    private UUID id;
    private UUID batchId;
    private String batchIdCode;
    private LocalDate reportDate;
    private PackingWeightType packingWeightType;
    private Integer boxesPacked;
    private Integer damagedBoxes;
    private List<String> damagedBoxPhotoUrls;
    private BigDecimal wastageWeightKg;
    private Integer boxesWasted;
    private String wastagePhotoUrl;
    // Transport
    private String vehicleNumber;
    private BigDecimal odometerStartKm;
    private BigDecimal odometerEndKm;
    private BigDecimal distanceKm;       // computed
    private BigDecimal ratePerKm;
    private BigDecimal transportCost;    // computed
    private BigDecimal tollAmount;
    private String weighBridgePhotoUrl;
    // Labor
    private Integer laborCount;
    private BigDecimal laborCost;
    private String laborPaymentStatus;
    private String notes;
    private LocalDateTime createdAt;
}
