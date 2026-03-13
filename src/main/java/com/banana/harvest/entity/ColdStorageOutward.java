package com.banana.harvest.entity;

import com.banana.harvest.entity.enums.Destination;
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
@Table(name = "cold_storage_outwards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ColdStorageOutward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "container_number", nullable = false)
    private String containerNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Destination destination;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

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
