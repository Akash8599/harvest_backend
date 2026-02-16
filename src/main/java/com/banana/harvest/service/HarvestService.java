package com.banana.harvest.service;

import com.banana.harvest.dto.harvest.*;
import com.banana.harvest.entity.*;
import com.banana.harvest.entity.enums.BatchStatus;
import com.banana.harvest.entity.enums.PaymentStatus;
import com.banana.harvest.entity.enums.TransportType;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HarvestService {

        private final DailyHarvestReportRepository reportRepository;
        private final LaborCostRepository laborCostRepository;
        private final TransportCostRepository transportCostRepository;
        private final GatePassRepository gatePassRepository;
        private final BatchRepository batchRepository;
        private final UserRepository userRepository;
        private final BatchCostRepository batchCostRepository;
        private final InventoryAllocationRepository allocationRepository;

        @Transactional
        public DailyHarvestResponse createDailyReport(DailyHarvestRequest request, UUID userId) {
                log.info("Creating daily harvest report - batchId: {}, reportDate: {}, boxesPacked: {}, boxesWasted: {}, laborCount: {}",
                                request.getBatchId(), request.getReportDate(), request.getBoxesPacked(),
                                request.getBoxesWasted(), request.getLaborCount());

                Batch batch = batchRepository.findById(UUID.fromString(request.getBatchId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", request.getBatchId()));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                if (batch.getDispatchedBoxes() == null) {
                        batch.setDispatchedBoxes(0);
                }
                if (batch.getGatePassRemaining() == null) {
                        batch.setGatePassRemaining(batch.getHarvestedBoxes() - batch.getDispatchedBoxes());
                }
                if (batch.getAllocatedBoxes() == null) {
                        batch.setAllocatedBoxes(0);
                }

                // Validate remaining harvest capacity
                if (request.getBoxesPacked() > batch.getRemainingBoxes()) {
                        throw new BusinessException(
                                        String.format("Limit exceeded. Only %d boxes remaining for harvest in this batch.",
                                                        batch.getRemainingBoxes()));
                }

                // Update harvested counts
                batch.setHarvestedBoxes(batch.getHarvestedBoxes() + request.getBoxesPacked());
                batch.setRemainingBoxes(batch.getAllocatedBoxes() - batch.getHarvestedBoxes());
                batch.setGatePassRemaining(batch.getHarvestedBoxes() - batch.getDispatchedBoxes());

                // Update actual boxes if needed (legacy field)
                batch.setActualBoxes(batch.getHarvestedBoxes());

                // Update batch status
                if (batch.getHarvestedBoxes() > 0 && batch.getHarvestedBoxes() < batch.getAllocatedBoxes()) {
                        log.debug("Updating batch status to HARVEST_IN_PROGRESS - batchId: {}", batch.getId());
                        batch.setStatus(BatchStatus.HARVEST_IN_PROGRESS);
                } else if (batch.getHarvestedBoxes() >= batch.getAllocatedBoxes()) {
                        log.debug("Updating batch status to HARVEST_COMPLETED - batchId: {}", batch.getId());
                        batch.setStatus(BatchStatus.HARVEST_COMPLETED);
                }
                // Update batch status
                if (batch.getStatus() == BatchStatus.CREATED) {
                        log.debug("Updating batch status from CREATED to IN_PROGRESS - batchId: {}", batch.getId());
                        batch.setStatus(BatchStatus.IN_PROGRESS);
                        // batchRepository.save(batch); // Will be saved later with updated counts
                }

                DailyHarvestReport report = DailyHarvestReport.builder()
                                .batch(batch)
                                .reportDate(request.getReportDate())
                                .boxesPacked(request.getBoxesPacked())
                                .boxesWasted(request.getBoxesWasted())
                                .laborCount(request.getLaborCount())
                                .notes(request.getNotes())
                                .createdBy(user)
                                .build();

                DailyHarvestReport savedReport = reportRepository.save(report);

                // Create labor cost if provided
                if (request.getLaborCost() != null && request.getLaborCost().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal costPerBox = request.getLaborCost()
                                        .divide(BigDecimal.valueOf(request.getBoxesPacked()), 2, RoundingMode.HALF_UP);

                        log.debug("Creating labor cost - totalAmount: {}, costPerBox: {}", request.getLaborCost(),
                                        costPerBox);

                        LaborCost laborCost = LaborCost.builder()
                                        .report(savedReport)
                                        .batch(batch)
                                        .totalAmount(request.getLaborCost())
                                        .costPerBox(costPerBox)
                                        .createdBy(user)
                                        .paymentStatus(PaymentStatus.PENDING)
                                        .build();
                        laborCostRepository.save(laborCost);
                }

                // Update batch box counts
                Integer totalPacked = reportRepository.sumBoxesPackedByBatch(batch.getId());
                batch.setHarvestedBoxes(totalPacked);
                batch.setRemainingBoxes(batch.getAllocatedBoxes() - totalPacked);
                batch.setActualBoxes(totalPacked);
                batchRepository.save(batch);

                // Recalculate batch costs
                recalculateBatchCosts(batch);

                batchRepository.save(batch);

                log.info("Daily report created - reportId: {}, batchId: {}, boxesPacked: {}",
                                savedReport.getId(), batch.getId(), savedReport.getBoxesPacked());

                return mapToDailyHarvestResponse(savedReport);
        }

        @Transactional(readOnly = true)
        public List<DailyHarvestResponse> getBatchReports(UUID batchId) {
                return reportRepository.findByBatchOrderByDate(batchId).stream()
                                .map(this::mapToDailyHarvestResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<DailyHarvestResponse> getTodayReports() {
                return reportRepository.findByReportDate(LocalDate.now()).stream()
                                .map(this::mapToDailyHarvestResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void addTransportCost(TransportCostRequest request, UUID userId) {
                log.info("Adding transport cost - batchId: {}, costType: {}, totalCost: {}, vehicleNumber: {}",
                                request.getBatchId(), request.getCostType(), request.getTotalCost(),
                                request.getVehicleNumber());

                Batch batch = batchRepository.findById(UUID.fromString(request.getBatchId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", request.getBatchId()));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                // Calculate cost per box based on actual boxes or estimated if not available
                Integer boxCount = batch.getActualBoxes() != null && batch.getActualBoxes() > 0
                                ? batch.getActualBoxes()
                                : batch.getEstimatedBoxes();

                BigDecimal costPerBox = request.getTotalCost()
                                .divide(BigDecimal.valueOf(boxCount), 2, RoundingMode.HALF_UP);

                log.debug("Transport cost calculation - boxCount: {}, totalCost: {}, costPerBox: {}",
                                boxCount, request.getTotalCost(), costPerBox);

                TransportCost transportCost = TransportCost.builder()
                                .batch(batch)
                                .costType(request.getCostType())
                                .vendorName(request.getVendorName())
                                .vehicleNumber(request.getVehicleNumber())
                                .driverName(request.getDriverName())
                                .driverPhone(request.getDriverPhone())
                                .totalCost(request.getTotalCost())
                                .costPerBox(costPerBox)
                                .distanceKm(request.getDistanceKm())
                                .notes(request.getNotes())
                                .createdBy(user)
                                .build();

                transportCostRepository.save(transportCost);
                log.info("Transport cost added successfully - batchId: {}, costType: {}", batch.getId(),
                                request.getCostType());

                // Recalculate batch costs
                log.debug("Recalculating batch costs for batchId: {}", batch.getId());
                recalculateBatchCosts(batch);
        }

        @Transactional
        public GatePassResponse createGatePass(GatePassRequest request, UUID userId) {
                log.info("Creating gate pass - batchId: {}, truckNumber: {}, totalBoxes: {}, dispatchDate: {}",
                                request.getBatchId(), request.getTruckNumber(), request.getTotalBoxes(),
                                request.getDispatchDate());

                Batch batch = batchRepository.findById(UUID.fromString(request.getBatchId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", request.getBatchId()));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                // Ensure box tracking fields are not null (for legacy data)
                if (batch.getHarvestedBoxes() == null) {
                        batch.setHarvestedBoxes(batch.getActualBoxes() != null ? batch.getActualBoxes() : 0);
                }
                if (batch.getDispatchedBoxes() == null) {
                        batch.setDispatchedBoxes(0);
                }
                if (batch.getGatePassRemaining() == null) {
                        batch.setGatePassRemaining(batch.getHarvestedBoxes() - batch.getDispatchedBoxes());
                }
                if (batch.getAllocatedBoxes() == null) {
                        batch.setAllocatedBoxes(0);
                }

                // Validate gate pass capacity
                if (request.getTotalBoxes() > batch.getGatePassRemaining()) {
                        throw new BusinessException(
                                        String.format("Limit exceeded. Only %d boxes available for dispatch in this batch.",
                                                        batch.getGatePassRemaining()));
                }

                // Update dispatch counts
                batch.setDispatchedBoxes(batch.getDispatchedBoxes() + request.getTotalBoxes());
                batch.setGatePassRemaining(batch.getHarvestedBoxes() - batch.getDispatchedBoxes());

                // Update batch status
                if (batch.getDispatchedBoxes() > 0 && batch.getDispatchedBoxes() < batch.getHarvestedBoxes()) {
                        log.debug("Updating batch status to DISPATCH_IN_PROGRESS - batchId: {}", batch.getId());
                        batch.setStatus(BatchStatus.DISPATCH_IN_PROGRESS);
                } else if (batch.getDispatchedBoxes() >= batch.getHarvestedBoxes()) {
                        if (batch.getHarvestedBoxes() >= batch.getAllocatedBoxes()) {
                                log.debug("Updating batch status to DISPATCH_COMPLETED - batchId: {}", batch.getId());
                                batch.setStatus(BatchStatus.DISPATCH_COMPLETED);
                        } else {
                                log.debug("Dispatch caught up with harvest - batchId: {}", batch.getId());
                                batch.setStatus(BatchStatus.HARVEST_IN_PROGRESS);
                        }
                }

                String gatePassNo = generateGatePassNo();
                log.debug("Generated gate pass number: {}", gatePassNo);

                GatePass gatePass = GatePass.builder()
                                .batch(batch)
                                .gatePassNo(gatePassNo)
                                .truckNumber(request.getTruckNumber())
                                .driverName(request.getDriverName())
                                .driverPhone(request.getDriverPhone())
                                .totalBoxes(request.getTotalBoxes())
                                .dispatchDate(request.getDispatchDate().atStartOfDay())
                                .notes(request.getNotes())
                                .createdBy(user)
                                .build();

                GatePass savedGatePass = gatePassRepository.save(gatePass);
                log.info("Gate pass created - gatePassId: {}, gatePassNo: {}, truckNumber: {}",
                                savedGatePass.getId(), gatePassNo, request.getTruckNumber());

                return mapToGatePassResponse(savedGatePass);
        }

        @Transactional
        public GatePassResponse receiveGatePass(UUID gatePassId, Integer receivedBoxes, UUID receivedById) {
                log.info("Receiving gate pass - gatePassId: {}, receivedBoxes: {}, receivedBy: {}",
                                gatePassId, receivedBoxes, receivedById);

                GatePass gatePass = gatePassRepository.findById(gatePassId)
                                .orElseThrow(() -> new ResourceNotFoundException("Gate pass", "id", gatePassId));

                User receivedBy = userRepository.findById(receivedById)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", receivedById));

                Integer shortage = gatePass.getTotalBoxes() - receivedBoxes;
                log.debug("Gate pass details - gatePassNo: {}, dispatchedBoxes: {}, receivedBoxes: {}, shortage: {}",
                                gatePass.getGatePassNo(), gatePass.getTotalBoxes(), receivedBoxes, shortage);

                gatePass.setReceivedBoxes(receivedBoxes);
                gatePass.setReceivedAt(LocalDateTime.now());
                gatePass.setReceivedBy(receivedBy);

                GatePass savedGatePass = gatePassRepository.save(gatePass);

                if (shortage > 0) {
                        log.warn("Gate pass received with shortage - gatePassId: {}, shortage: {} boxes", gatePassId,
                                        shortage);
                } else {
                        log.info("Gate pass received successfully - gatePassId: {}, no shortage", gatePassId);
                }

                return mapToGatePassResponse(savedGatePass);
        }

        @Transactional(readOnly = true)
        public List<GatePassResponse> getBatchGatePasses(UUID batchId) {
                return gatePassRepository.findByBatchId(batchId).stream()
                                .map(this::mapToGatePassResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<GatePassResponse> getGatePassesByDate(LocalDate date) {
                log.info("Fetching gate passes for date: {}", date);
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                return gatePassRepository.findByDispatchDateBetween(startOfDay, endOfDay).stream()
                                .map(this::mapToGatePassResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<DailyHarvestResponse> getReportsByDate(LocalDate date) {
                log.info("Fetching harvest reports for date: {}", date);
                return reportRepository.findByReportDate(date).stream()
                                .map(this::mapToDailyHarvestResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<GatePassResponse> getPendingGatePasses() {
                log.info("Fetching pending gate passes");
                return gatePassRepository.findPendingGatePasses().stream()
                                .map(this::mapToGatePassResponse)
                                .collect(Collectors.toList());
        }

        private GatePassResponse mapToGatePassResponse(GatePass gatePass) {
                return GatePassResponse.builder()
                                .id(gatePass.getId())
                                .batchId(gatePass.getBatch().getId())
                                .batchIdCode(gatePass.getBatch().getBatchId())
                                .gatePassNo(gatePass.getGatePassNo())
                                .truckNumber(gatePass.getTruckNumber())
                                .driverName(gatePass.getDriverName())
                                .driverPhone(gatePass.getDriverPhone())
                                .totalBoxes(gatePass.getTotalBoxes())
                                .dispatchDate(gatePass.getDispatchDate())
                                .receivedBoxes(gatePass.getReceivedBoxes())
                                .receivedAt(gatePass.getReceivedAt())
                                .receivedBy(gatePass.getReceivedBy() != null ? gatePass.getReceivedBy().getFullName()
                                                : null)
                                .notes(gatePass.getNotes())
                                .createdBy(gatePass.getCreatedBy() != null ? gatePass.getCreatedBy().getFullName()
                                                : null)
                                .createdAt(gatePass.getCreatedAt())
                                .build();
        }

        private void recalculateBatchCosts(Batch batch) {
                BatchCost batchCost = batchCostRepository.findByBatchId(batch.getId())
                                .orElseGet(() -> BatchCost.builder().batch(batch).build());

                // Calculate material costs from allocations
                List<InventoryAllocation> allocations = allocationRepository.findByBatchId(batch.getId());
                BigDecimal materialCostTotal = allocations.stream()
                                .map(a -> a.getItem().getUnitCost().multiply(BigDecimal.valueOf(a.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Get transport costs
                BigDecimal outwardTransport = transportCostRepository
                                .sumCostByBatchAndType(batch.getId(), TransportType.OUTWARD);
                BigDecimal inwardTransport = transportCostRepository
                                .sumCostByBatchAndType(batch.getId(), TransportType.INWARD);

                // Get labor costs
                BigDecimal laborCostTotal = laborCostRepository.sumTotalAmountByBatch(batch.getId());

                // Calculate totals
                Integer boxCount = batch.getActualBoxes() != null && batch.getActualBoxes() > 0
                                ? batch.getActualBoxes()
                                : batch.getEstimatedBoxes();

                BigDecimal totalCost = materialCostTotal.add(outwardTransport).add(laborCostTotal).add(inwardTransport);
                BigDecimal finalCostPerBox = boxCount > 0
                                ? totalCost.divide(BigDecimal.valueOf(boxCount), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                // Update batch cost
                batchCost.setMaterialCostTotal(materialCostTotal);
                batchCost.setMaterialCostPerBox(boxCount > 0
                                ? materialCostTotal.divide(BigDecimal.valueOf(boxCount), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO);
                batchCost.setOutwardTransportCost(outwardTransport);
                batchCost.setOutwardTransportPerBox(boxCount > 0
                                ? outwardTransport.divide(BigDecimal.valueOf(boxCount), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO);
                batchCost.setLaborCostTotal(laborCostTotal);
                batchCost.setLaborCostPerBox(boxCount > 0
                                ? laborCostTotal.divide(BigDecimal.valueOf(boxCount), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO);
                batchCost.setInwardTransportCost(inwardTransport);
                batchCost.setInwardTransportPerBox(boxCount > 0
                                ? inwardTransport.divide(BigDecimal.valueOf(boxCount), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO);
                batchCost.setTotalCost(totalCost);
                batchCost.setFinalCostPerBox(finalCostPerBox);
                batchCost.setCalculatedAt(LocalDateTime.now());

                batchCostRepository.save(batchCost);
        }

        private String generateGatePassNo() {
                String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                long count = gatePassRepository.count() + 1;
                return "GP-" + datePrefix + "-" + String.format("%04d", count);
        }

        private DailyHarvestResponse mapToDailyHarvestResponse(DailyHarvestReport report) {
                LaborCost laborCost = laborCostRepository.findByBatch(report.getBatch().getId()).stream()
                                .filter(lc -> lc.getReport().getId().equals(report.getId()))
                                .findFirst()
                                .orElse(null);

                return DailyHarvestResponse.builder()
                                .id(report.getId())
                                .batchId(report.getBatch().getId())
                                .batchIdCode(report.getBatch().getBatchId())
                                .reportDate(report.getReportDate())
                                .boxesPacked(report.getBoxesPacked())
                                .boxesWasted(report.getBoxesWasted())
                                .laborCount(report.getLaborCount())
                                .notes(report.getNotes())
                                .laborCost(laborCost != null ? laborCost.getTotalAmount() : null)
                                .laborPaymentStatus(laborCost != null ? laborCost.getPaymentStatus().name() : null)
                                .createdAt(report.getCreatedAt())
                                .build();
        }
}
