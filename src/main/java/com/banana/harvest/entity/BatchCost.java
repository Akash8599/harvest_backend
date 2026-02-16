package com.banana.harvest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_costs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BatchCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", unique = true)
    private Batch batch;

    @Column(name = "material_cost_total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal materialCostTotal = BigDecimal.ZERO;

    @Column(name = "material_cost_per_box", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal materialCostPerBox = BigDecimal.ZERO;

    @Column(name = "outward_transport_cost", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal outwardTransportCost = BigDecimal.ZERO;

    @Column(name = "outward_transport_per_box", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal outwardTransportPerBox = BigDecimal.ZERO;

    @Column(name = "labor_cost_total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal laborCostTotal = BigDecimal.ZERO;

    @Column(name = "labor_cost_per_box", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal laborCostPerBox = BigDecimal.ZERO;

    @Column(name = "inward_transport_cost", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal inwardTransportCost = BigDecimal.ZERO;

    @Column(name = "inward_transport_per_box", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal inwardTransportPerBox = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "final_cost_per_box", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal finalCostPerBox = BigDecimal.ZERO;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Manual Builder
    public static BatchCostBuilder builder() {
        return new BatchCostBuilder();
    }

    public static class BatchCostBuilder {
        private UUID id;
        private Batch batch;
        private BigDecimal materialCostTotal = BigDecimal.ZERO;
        private BigDecimal materialCostPerBox = BigDecimal.ZERO;
        private BigDecimal outwardTransportCost = BigDecimal.ZERO;
        private BigDecimal outwardTransportPerBox = BigDecimal.ZERO;
        private BigDecimal laborCostTotal = BigDecimal.ZERO;
        private BigDecimal laborCostPerBox = BigDecimal.ZERO;
        private BigDecimal inwardTransportCost = BigDecimal.ZERO;
        private BigDecimal inwardTransportPerBox = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private BigDecimal finalCostPerBox = BigDecimal.ZERO;
        private LocalDateTime calculatedAt;

        public BatchCostBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public BatchCostBuilder batch(Batch batch) {
            this.batch = batch;
            return this;
        }

        public BatchCostBuilder materialCostTotal(BigDecimal materialCostTotal) {
            this.materialCostTotal = materialCostTotal;
            return this;
        }

        public BatchCostBuilder materialCostPerBox(BigDecimal materialCostPerBox) {
            this.materialCostPerBox = materialCostPerBox;
            return this;
        }

        public BatchCostBuilder outwardTransportCost(BigDecimal outwardTransportCost) {
            this.outwardTransportCost = outwardTransportCost;
            return this;
        }

        public BatchCostBuilder outwardTransportPerBox(BigDecimal outwardTransportPerBox) {
            this.outwardTransportPerBox = outwardTransportPerBox;
            return this;
        }

        public BatchCostBuilder laborCostTotal(BigDecimal laborCostTotal) {
            this.laborCostTotal = laborCostTotal;
            return this;
        }

        public BatchCostBuilder laborCostPerBox(BigDecimal laborCostPerBox) {
            this.laborCostPerBox = laborCostPerBox;
            return this;
        }

        public BatchCostBuilder inwardTransportCost(BigDecimal inwardTransportCost) {
            this.inwardTransportCost = inwardTransportCost;
            return this;
        }

        public BatchCostBuilder inwardTransportPerBox(BigDecimal inwardTransportPerBox) {
            this.inwardTransportPerBox = inwardTransportPerBox;
            return this;
        }

        public BatchCostBuilder totalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
            return this;
        }

        public BatchCostBuilder finalCostPerBox(BigDecimal finalCostPerBox) {
            this.finalCostPerBox = finalCostPerBox;
            return this;
        }

        public BatchCostBuilder calculatedAt(LocalDateTime calculatedAt) {
            this.calculatedAt = calculatedAt;
            return this;
        }

        public BatchCost build() {
            return new BatchCost(id, batch, materialCostTotal, materialCostPerBox, outwardTransportCost,
                    outwardTransportPerBox, laborCostTotal, laborCostPerBox, inwardTransportCost, inwardTransportPerBox,
                    totalCost, finalCostPerBox, calculatedAt, null);
        }
    }

    // Manual Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public BigDecimal getMaterialCostTotal() {
        return materialCostTotal;
    }

    public void setMaterialCostTotal(BigDecimal materialCostTotal) {
        this.materialCostTotal = materialCostTotal;
    }

    public BigDecimal getMaterialCostPerBox() {
        return materialCostPerBox;
    }

    public void setMaterialCostPerBox(BigDecimal materialCostPerBox) {
        this.materialCostPerBox = materialCostPerBox;
    }

    public BigDecimal getOutwardTransportCost() {
        return outwardTransportCost;
    }

    public void setOutwardTransportCost(BigDecimal outwardTransportCost) {
        this.outwardTransportCost = outwardTransportCost;
    }

    public BigDecimal getOutwardTransportPerBox() {
        return outwardTransportPerBox;
    }

    public void setOutwardTransportPerBox(BigDecimal outwardTransportPerBox) {
        this.outwardTransportPerBox = outwardTransportPerBox;
    }

    public BigDecimal getLaborCostTotal() {
        return laborCostTotal;
    }

    public void setLaborCostTotal(BigDecimal laborCostTotal) {
        this.laborCostTotal = laborCostTotal;
    }

    public BigDecimal getLaborCostPerBox() {
        return laborCostPerBox;
    }

    public void setLaborCostPerBox(BigDecimal laborCostPerBox) {
        this.laborCostPerBox = laborCostPerBox;
    }

    public BigDecimal getInwardTransportCost() {
        return inwardTransportCost;
    }

    public void setInwardTransportCost(BigDecimal inwardTransportCost) {
        this.inwardTransportCost = inwardTransportCost;
    }

    public BigDecimal getInwardTransportPerBox() {
        return inwardTransportPerBox;
    }

    public void setInwardTransportPerBox(BigDecimal inwardTransportPerBox) {
        this.inwardTransportPerBox = inwardTransportPerBox;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getFinalCostPerBox() {
        return finalCostPerBox;
    }

    public void setFinalCostPerBox(BigDecimal finalCostPerBox) {
        this.finalCostPerBox = finalCostPerBox;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
