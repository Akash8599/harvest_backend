package com.banana.harvest.dto.harvest;

import com.banana.harvest.entity.enums.PackingWeightType;
import com.banana.harvest.entity.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyHarvestRequest {

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Report date is required")
    private LocalDate reportDate;

    private PackingWeightType packingWeightType;

    @NotNull(message = "Boxes packed is required")
    private Integer boxesPacked;

    private Integer damagedBoxes;
    private List<String> damagedBoxPhotoUrls;
    private BigDecimal wastageWeightKg;

    @NotNull(message = "Boxes wasted is required")
    private Integer boxesWasted;

    private String wastagePhotoUrl;

    // Transport / odometer
    private String vehicleNumber;
    private BigDecimal odometerStartKm;
    private BigDecimal odometerEndKm;
    private String odometerStartPhotoUrl;
    private String odometerEndPhotoUrl;
    private BigDecimal ratePerKm;
    private BigDecimal tollAmount;
    private String tollReceiptPhotoUrl;
    private String weighBridgePhotoUrl;

    // Labor
    @NotNull(message = "Labor count is required")
    private Integer laborCount;

    private BigDecimal laborCost;
    private PaymentStatus laborPaymentStatus;

    private String notes;
}
