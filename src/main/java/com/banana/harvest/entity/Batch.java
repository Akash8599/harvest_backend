package com.banana.harvest.entity;

import com.banana.harvest.entity.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", unique = true, nullable = false, length = 50)
    private String batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id")
    private FarmInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private User vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BatchStatus status = BatchStatus.CREATED;

    @Column(name = "estimated_boxes", nullable = false)
    private Integer estimatedBoxes;

    @Column(name = "allocated_boxes")
    @Builder.Default
    private Integer allocatedBoxes = 0;

    @Column(name = "harvested_boxes")
    @Builder.Default
    private Integer harvestedBoxes = 0;

    @Column(name = "remaining_boxes")
    @Builder.Default
    private Integer remainingBoxes = 0;

    @Column(name = "actual_boxes")
    @Builder.Default
    private Integer actualBoxes = 0;

    @Column(name = "dispatched_boxes")
    @Builder.Default
    private Integer dispatchedBoxes = 0;

    @Column(name = "gate_pass_remaining")
    @Builder.Default
    private Integer gatePassRemaining = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
