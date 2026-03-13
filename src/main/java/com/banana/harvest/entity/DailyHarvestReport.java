package com.banana.harvest.entity;

import com.banana.harvest.entity.enums.PackingWeightType;
import com.banana.harvest.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_harvest_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DailyHarvestReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "packing_weight_type")
    private PackingWeightType packingWeightType;

    @Column(name = "boxes_packed", nullable = false)
    @Builder.Default
    private Integer boxesPacked = 0;

    @Column(name = "damaged_boxes")
    @Builder.Default
    private Integer damagedBoxes = 0;

    @Column(name = "damaged_box_photo_urls", columnDefinition = "TEXT")
    private String damagedBoxPhotoUrls; // JSON array stored as text

    @Column(name = "wastage_weight_kg", precision = 8, scale = 2)
    private BigDecimal wastageWeightKg;

    @Column(name = "boxes_wasted")
    @Builder.Default
    private Integer boxesWasted = 0;

    @Column(name = "wastage_photo_url", length = 500)
    private String wastagePhotoUrl;

    // Transport / odometer
    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "odometer_start_km", precision = 10, scale = 2)
    private BigDecimal odometerStartKm;

    @Column(name = "odometer_end_km", precision = 10, scale = 2)
    private BigDecimal odometerEndKm;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm; // computed: odometerEndKm - odometerStartKm

    @Column(name = "odometer_start_photo_url", length = 500)
    private String odometerStartPhotoUrl;

    @Column(name = "odometer_end_photo_url", length = 500)
    private String odometerEndPhotoUrl;

    @Column(name = "rate_per_km", precision = 8, scale = 2)
    private BigDecimal ratePerKm;

    @Column(name = "transport_cost", precision = 10, scale = 2)
    private BigDecimal transportCost; // computed: distanceKm * ratePerKm

    @Column(name = "toll_amount", precision = 10, scale = 2)
    private BigDecimal tollAmount;

    @Column(name = "toll_receipt_photo_url", length = 500)
    private String tollReceiptPhotoUrl;

    @Column(name = "weigh_bridge_photo_url", length = 500)
    private String weighBridgePhotoUrl;

    // Labor
    @Column(name = "labor_count")
    @Builder.Default
    private Integer laborCount = 0;

    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "labor_payment_status")
    @Builder.Default
    private PaymentStatus laborPaymentStatus = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
