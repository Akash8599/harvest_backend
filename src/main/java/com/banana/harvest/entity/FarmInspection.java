package com.banana.harvest.entity;

import com.banana.harvest.entity.enums.InspectionStatus;
import com.banana.harvest.entity.enums.RateStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "farm_inspections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FarmInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private User vendor;

    @Column(name = "estimated_boxes")
    private Integer estimatedBoxes;

    @Column(name = "allocated_boxes")
    private Integer allocatedBoxes;

    @Column(name = "liners_qty")
    private Integer linersQty;

    @Column(name = "corners_qty")
    private Integer cornersQty;

    @Column(name = "tape_rolls")
    private Integer tapeRolls;

    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    @Column(name = "proposed_rate", precision = 10, scale = 2)
    private BigDecimal proposedRate;

    @Column(name = "farmer_proposed_rate", precision = 10, scale = 2)
    private BigDecimal farmerProposedRate;

    @Column(name = "admin_counter_rate", precision = 10, scale = 2)
    private BigDecimal adminCounterRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_status")
    @Builder.Default
    private RateStatus rateStatus = RateStatus.PENDING;

    @Column(name = "negotiation_notes", columnDefinition = "TEXT")
    private String negotiationNotes;

    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "gps_latitude", precision = 10, scale = 8)
    private BigDecimal gpsLatitude;

    @Column(name = "gps_longitude", precision = 11, scale = 8)
    private BigDecimal gpsLongitude;

    @Column(name = "gps_accuracy", precision = 5, scale = 2)
    private BigDecimal gpsAccuracy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InspectionStatus status = InspectionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FarmPhoto> photos = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void addPhoto(FarmPhoto photo) {
        photos.add(photo);
        photo.setInspection(this);
    }

    public void removePhoto(FarmPhoto photo) {
        photos.remove(photo);
        photo.setInspection(null);
    }
}
