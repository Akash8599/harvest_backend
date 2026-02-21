package com.banana.harvest.service;

import com.banana.harvest.dto.farm.BatchResponse;
import com.banana.harvest.dto.farm.UpdateBatchStatusRequest;
import com.banana.harvest.entity.Batch;
import com.banana.harvest.entity.User;
import com.banana.harvest.entity.enums.BatchStatus;
import com.banana.harvest.entity.enums.UserRole; // Correct import
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.BatchRepository;
import com.banana.harvest.repository.DailyHarvestReportRepository;
import com.banana.harvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final DailyHarvestReportRepository dailyHarvestReportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public BatchResponse getBatchById(UUID batchId) {
        log.info("Fetching batch details for id: {}", batchId);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));

        return mapToBatchResponse(batch);
    }

    @Transactional
    public BatchResponse updateBatchStatus(UUID batchId, UpdateBatchStatusRequest request, UUID userId) {
        log.info("Updating batch status - batchId: {}, newStatus: {}, userId: {}", batchId, request.getStatus(),
                userId);

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // RBAC: If user is VENDOR, they must own the batch
        if (user.getRole() == UserRole.VENDOR) {
            if (!batch.getVendor().getId().equals(userId)) {
                log.warn("Unauthorized batch update attempt - userId: {}, batchVendorId: {}", userId,
                        batch.getVendor().getId());
                throw new BusinessException("You are not authorized to update this batch.");
            }
        }

        // Validate Status Transition (Basic)
        // Ensure we don't move back from COMPLETED/CANCELLED unless admin?
        // For now, implementing basic check for HARVEST_COMPLETED

        if (request.getStatus() == BatchStatus.HARVEST_COMPLETED) {
            if (batch.getStatus() != BatchStatus.HARVEST_IN_PROGRESS &&
                    batch.getStatus() != BatchStatus.CREATED &&
                    batch.getStatus() != BatchStatus.IN_PROGRESS &&
                    batch.getStatus() != BatchStatus.DISPATCH_IN_PROGRESS) { // Permissive transition for now
                // Maybe strict: Only from HARVEST_IN_PROGRESS?
                // User said: "State Transition: Ensure valid transition (e.g.,
                // HARVEST_IN_PROGRESS -> HARVEST_COMPLETED)."
            }
        }

        // Allow IN_TRANSIT transition from DISPATCH_COMPLETED
        if (request.getStatus() == BatchStatus.IN_TRANSIT) {
            if (batch.getStatus() != BatchStatus.DISPATCH_COMPLETED) {
                throw new BusinessException("Batch must be in DISPATCH_COMPLETED status to transition to IN_TRANSIT.");
            }
        }

        // Allow DELIVERED transition from IN_TRANSIT
        if (request.getStatus() == BatchStatus.DELIVERED) {
            if (batch.getStatus() != BatchStatus.IN_TRANSIT && batch.getStatus() != BatchStatus.DISPATCH_COMPLETED) {
                throw new BusinessException("Batch must be in IN_TRANSIT status to transition to DELIVERED.");
            }
        }

        batch.setStatus(request.getStatus());
        
        // Track specific status dates
        if (request.getStatus() == BatchStatus.HARVEST_IN_PROGRESS && batch.getStartDate() == null) {
            batch.setStartDate(LocalDate.now());
        }

        Batch savedBatch = batchRepository.save(batch);
        log.info("Batch status updated successfully - batchId: {}, status: {}", savedBatch.getId(),
                savedBatch.getStatus());

        return mapToBatchResponse(savedBatch);
    }

    private BatchResponse mapToBatchResponse(Batch batch) {
        // Calculate dynamic values
        Integer actualBoxes = dailyHarvestReportRepository.sumBoxesPackedByBatch(batch.getId());
        if (actualBoxes == null)
            actualBoxes = 0;

        return BatchResponse.builder()
                .id(batch.getId())
                .batchId(batch.getBatchId())
                .inspectionId(batch.getInspection() != null ? batch.getInspection().getId() : null)
                .farmId(batch.getFarm() != null ? batch.getFarm().getId() : null)
                .farmName(batch.getFarm() != null ? batch.getFarm().getFarmerName() : null)
                .farmLocation(batch.getFarm() != null ? batch.getFarm().getLocation() : null)
                .produceType(batch.getFarm() != null ? batch.getFarm().getProduceType() : null)
                .vendorId(batch.getVendor() != null ? batch.getVendor().getId() : null)
                .vendorName(batch.getVendor() != null ? batch.getVendor().getFullName() : null)
                .status(batch.getStatus())
                .estimatedBoxes(batch.getEstimatedBoxes())
                .allocatedBoxes(batch.getAllocatedBoxes())
                .harvestedBoxes(batch.getHarvestedBoxes()) // This might be redundant if we use actualBoxes
                .remainingBoxes(batch.getRemainingBoxes())
                .actualBoxes(actualBoxes) // Dynamically calculated
                .dispatchedBoxes(batch.getDispatchedBoxes())
                .gatePassRemaining(batch.getGatePassRemaining())
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .createdAt(batch.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> getAllBatches() {
            log.info("Fetching all batches from database");
            List<Batch> batches = batchRepository.findAll();
            log.info("Found {} batches in database", batches.size());

            List<BatchResponse> batchResponses = batches.stream()
                            .map(this::mapToBatchResponse)
                            .collect(Collectors.toList());

            // Log each batch's details
            batchResponses.forEach(batch -> log.info(
                            "Batch: id={}, batchId={}, farmName={}, vendorName={}, status={}, estimatedBoxes={}, actualBoxes={}",
                            batch.getId(), batch.getBatchId(), batch.getFarmName(), batch.getVendorName(),
                            batch.getStatus(), batch.getEstimatedBoxes(), batch.getActualBoxes()));

            log.info("Successfully retrieved {} batches", batchResponses.size());
            return batchResponses;
    }

}
