package com.banana.harvest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cold_storage_inwards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ColdStorageInward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "cold_storage_name", nullable = false)
    private String coldStorageName;

    @Column(name = "inward_date", nullable = false)
    private LocalDate inwardDate;

    // Based on breakdown (Kg 13, Kg 13.5, Kg 7, etc)
    @Column(name = "kg_13_boxes")
    @Builder.Default
    private Integer kg13Boxes = 0;

    @Column(name = "kg_13_5_boxes")
    @Builder.Default
    private Integer kg13_5Boxes = 0;

    @Column(name = "kg_7_boxes")
    @Builder.Default
    private Integer kg7Boxes = 0;

    @Column(name = "kg_16_boxes")
    @Builder.Default
    private Integer kg16Boxes = 0;

    @Column(name = "total_boxes", nullable = false)
    private Integer totalBoxes; // Computed

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
