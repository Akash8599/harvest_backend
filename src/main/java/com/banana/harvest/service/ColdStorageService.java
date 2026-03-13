package com.banana.harvest.service;

import com.banana.harvest.dto.coldstorage.*;
import com.banana.harvest.entity.Batch;
import com.banana.harvest.entity.ColdStorageInward;
import com.banana.harvest.entity.ColdStorageOutward;
import com.banana.harvest.entity.User;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.BatchRepository;
import com.banana.harvest.repository.ColdStorageInwardRepository;
import com.banana.harvest.repository.ColdStorageOutwardRepository;
import com.banana.harvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColdStorageService {

    private final ColdStorageInwardRepository inwardRepository;
    private final ColdStorageOutwardRepository outwardRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    // --- Inward Operations ---

    @Transactional
    public ColdStorageInwardResponse createInward(ColdStorageInwardRequest request, UUID userId) {
        log.info("Creating Cold Storage Inward record for batch: {}", request.getBatchId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Batch batch = batchRepository.findByBatchId(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "batchId", request.getBatchId()));

        int totalBoxes = calculateTotalBoxes(
                request.getKg13Boxes(),
                request.getKg13_5Boxes(),
                request.getKg7Boxes(),
                request.getKg16Boxes()
        );

        if (totalBoxes <= 0) {
            throw new BusinessException("Total inward boxes must be greater than zero");
        }

        ColdStorageInward inward = ColdStorageInward.builder()
                .batch(batch)
                .coldStorageName(request.getColdStorageName())
                .inwardDate(request.getInwardDate())
                .kg13Boxes(request.getKg13Boxes() != null ? request.getKg13Boxes() : 0)
                .kg13_5Boxes(request.getKg13_5Boxes() != null ? request.getKg13_5Boxes() : 0)
                .kg7Boxes(request.getKg7Boxes() != null ? request.getKg7Boxes() : 0)
                .kg16Boxes(request.getKg16Boxes() != null ? request.getKg16Boxes() : 0)
                .totalBoxes(totalBoxes)
                .remarks(request.getRemarks())
                .createdBy(user)
                .build();

        ColdStorageInward saved = inwardRepository.save(inward);
        log.info("Created Cold Storage Inward ID {} with {} boxes", saved.getId(), totalBoxes);

        return mapToInwardResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ColdStorageInwardResponse> getAllInwards() {
        log.info("Fetching all Cold Storage Inwards");
        return inwardRepository.findAll().stream()
                .map(this::mapToInwardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ColdStorageInwardResponse> getInwardsByBatch(UUID batchId) {
        log.info("Fetching Cold Storage Inwards for batch UUID: {}", batchId);
        return inwardRepository.findByBatchId(batchId).stream()
                .map(this::mapToInwardResponse)
                .collect(Collectors.toList());
    }

    // --- Outward Operations ---

    @Transactional
    public ColdStorageOutwardResponse createOutward(ColdStorageOutwardRequest request, UUID userId) {
        log.info("Creating Cold Storage Outward record to container: {}", request.getContainerNumber());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        int totalBoxes = calculateTotalBoxes(
                request.getKg13Boxes(),
                request.getKg13_5Boxes(),
                request.getKg7Boxes(),
                request.getKg16Boxes()
        );

        if (totalBoxes <= 0) {
            throw new BusinessException("Total outward boxes must be greater than zero");
        }

        if (totalBoxes > 1540) {
            log.warn("Validation failed: Outward total boxes {} exceeds maximum 1540", totalBoxes);
            throw new BusinessException("Total outward boxes cannot exceed 1540 per container.");
        }

        ColdStorageOutward outward = ColdStorageOutward.builder()
                .containerNumber(request.getContainerNumber())
                .destination(request.getDestination())
                .dispatchDate(request.getDispatchDate())
                .kg13Boxes(request.getKg13Boxes() != null ? request.getKg13Boxes() : 0)
                .kg13_5Boxes(request.getKg13_5Boxes() != null ? request.getKg13_5Boxes() : 0)
                .kg7Boxes(request.getKg7Boxes() != null ? request.getKg7Boxes() : 0)
                .kg16Boxes(request.getKg16Boxes() != null ? request.getKg16Boxes() : 0)
                .totalBoxes(totalBoxes)
                .remarks(request.getRemarks())
                .createdBy(user)
                .build();

        ColdStorageOutward saved = outwardRepository.save(outward);
        log.info("Created Cold Storage Outward ID {} with {} boxes", saved.getId(), totalBoxes);

        return mapToOutwardResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ColdStorageOutwardResponse> getAllOutwards() {
        log.info("Fetching all Cold Storage Outwards");
        return outwardRepository.findAll().stream()
                .map(this::mapToOutwardResponse)
                .collect(Collectors.toList());
    }

    // --- Inventory / Analytics ---

    @Transactional(readOnly = true)
    public ColdStorageInventoryResponse getInventory() {
        log.info("Calculating Cold Storage Inventory balance");
        Integer totalIn = inwardRepository.sumTotalInwardBoxes();
        Integer totalOut = outwardRepository.sumTotalOutwardBoxes();
        
        int balance = (totalIn != null ? totalIn : 0) - (totalOut != null ? totalOut : 0);

        return ColdStorageInventoryResponse.builder()
                .totalInwardBoxes(totalIn != null ? totalIn : 0)
                .totalOutwardBoxes(totalOut != null ? totalOut : 0)
                .balanceBoxes(balance)
                .build();
    }

    // --- Helpers ---

    private int calculateTotalBoxes(Integer b1, Integer b2, Integer b3, Integer b4) {
        return (b1 != null ? b1 : 0) +
               (b2 != null ? b2 : 0) +
               (b3 != null ? b3 : 0) +
               (b4 != null ? b4 : 0);
    }

    private ColdStorageInwardResponse mapToInwardResponse(ColdStorageInward inward) {
        return ColdStorageInwardResponse.builder()
                .id(inward.getId())
                .batchId(inward.getBatch().getId())
                .batchIdCode(inward.getBatch().getBatchId())
                .farmName(inward.getBatch().getFarm().getFarmerName())
                .coldStorageName(inward.getColdStorageName())
                .inwardDate(inward.getInwardDate())
                .kg13Boxes(inward.getKg13Boxes())
                .kg13_5Boxes(inward.getKg13_5Boxes())
                .kg7Boxes(inward.getKg7Boxes())
                .kg16Boxes(inward.getKg16Boxes())
                .totalBoxes(inward.getTotalBoxes())
                .remarks(inward.getRemarks())
                .createdByName(inward.getCreatedBy().getFullName())
                .createdAt(inward.getCreatedAt())
                .build();
    }

    private ColdStorageOutwardResponse mapToOutwardResponse(ColdStorageOutward outward) {
        return ColdStorageOutwardResponse.builder()
                .id(outward.getId())
                .containerNumber(outward.getContainerNumber())
                .destination(outward.getDestination())
                .dispatchDate(outward.getDispatchDate())
                .kg13Boxes(outward.getKg13Boxes())
                .kg13_5Boxes(outward.getKg13_5Boxes())
                .kg7Boxes(outward.getKg7Boxes())
                .kg16Boxes(outward.getKg16Boxes())
                .totalBoxes(outward.getTotalBoxes())
                .remarks(outward.getRemarks())
                .createdByName(outward.getCreatedBy().getFullName())
                .createdAt(outward.getCreatedAt())
                .build();
    }
}
