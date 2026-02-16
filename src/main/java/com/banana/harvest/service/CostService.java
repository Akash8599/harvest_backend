package com.banana.harvest.service;

import com.banana.harvest.dto.cost.BatchCostResponse;
import com.banana.harvest.entity.Batch;
import com.banana.harvest.entity.BatchCost;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.BatchCostRepository;
import com.banana.harvest.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostService {

    private final BatchCostRepository batchCostRepository;
    private final BatchRepository batchRepository;

    @Transactional(readOnly = true)
    public BatchCostResponse getBatchCost(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));

        BatchCost batchCost = batchCostRepository.findByBatchId(batchId)
                .orElseGet(() -> BatchCost.builder()
                        .batch(batch)
                        .build());

        return mapToBatchCostResponse(batchCost);
    }

    @Transactional(readOnly = true)
    public List<BatchCostResponse> getAllBatchCosts() {
        return batchCostRepository.findAll().stream()
                .map(this::mapToBatchCostResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BatchCostResponse getCostByBatchCode(String batchId) {
        Batch batch = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "batchId", batchId));

        BatchCost batchCost = batchCostRepository.findByBatchId(batch.getId())
                .orElseGet(() -> BatchCost.builder()
                        .batch(batch)
                        .build());

        return mapToBatchCostResponse(batchCost);
    }

    private BatchCostResponse mapToBatchCostResponse(BatchCost batchCost) {
        return BatchCostResponse.builder()
                .id(batchCost.getId())
                .batchId(batchCost.getBatch().getId())
                .batchIdCode(batchCost.getBatch().getBatchId())
                .materialCostTotal(batchCost.getMaterialCostTotal())
                .materialCostPerBox(batchCost.getMaterialCostPerBox())
                .outwardTransportCost(batchCost.getOutwardTransportCost())
                .outwardTransportPerBox(batchCost.getOutwardTransportPerBox())
                .laborCostTotal(batchCost.getLaborCostTotal())
                .laborCostPerBox(batchCost.getLaborCostPerBox())
                .inwardTransportCost(batchCost.getInwardTransportCost())
                .inwardTransportPerBox(batchCost.getInwardTransportPerBox())
                .totalCost(batchCost.getTotalCost())
                .finalCostPerBox(batchCost.getFinalCostPerBox())
                .calculatedAt(batchCost.getCalculatedAt())
                .build();
    }
}
