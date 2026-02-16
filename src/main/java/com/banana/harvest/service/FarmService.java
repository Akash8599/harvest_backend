package com.banana.harvest.service;

import com.banana.harvest.dto.farm.*;
import com.banana.harvest.entity.*;
import com.banana.harvest.entity.enums.BatchStatus;
import com.banana.harvest.entity.enums.InspectionStatus;
import com.banana.harvest.entity.enums.InspectionRequestStatus;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.exception.ResourceNotFoundException;
import com.banana.harvest.repository.*;
import com.banana.harvest.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class FarmService {

        private final FarmRepository farmRepository;
        private final FarmInspectionRepository inspectionRepository;
        private final InspectionRequestRepository inspectionRequestRepository;
        private final BatchRepository batchRepository;
        private final UserRepository userRepository;
        private final FarmPhotoRepository photoRepository;
        private final GpsValidationService gpsValidationService;
        private final PhotoValidationService photoValidationService;
        private final NotificationService notificationService;

        @Transactional
        public FarmResponse createFarm(FarmRequest request, UUID userId) {
                log.info("Creating farm - farmerName: {}, location: {}, totalArea: {} {}, createdBy: {}",
                                request.getFarmerName(), request.getLocation(), request.getTotalArea(),
                                request.getAreaUnit(), userId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                Farm farm = Farm.builder()
                                .farmerName(request.getFarmerName())
                                .location(request.getLocation())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .contactNumber(request.getContactNumber())
                                .totalArea(request.getTotalArea())
                                .totalArea(request.getTotalArea())
                                .areaUnit(request.getAreaUnit())
                                .produceType(request.getProduceType())
                                .createdBy(user)
                                .build();

                Farm savedFarm = farmRepository.save(farm);
                log.info("Farm created successfully - farmId: {}, farmerName: {}, location: {}",
                                savedFarm.getId(), savedFarm.getFarmerName(), savedFarm.getLocation());

                return mapToFarmResponse(savedFarm);
        }

        @Transactional(readOnly = true)
        public List<FarmResponse> getAllFarms() {
                log.info("Fetching all farms from database");
                List<Farm> farms = farmRepository.findAll();
                log.info("Found {} farms in database", farms.size());

                List<FarmResponse> farmResponses = farms.stream()
                                .map(this::mapToFarmResponse)
                                .collect(Collectors.toList());

                // Log each farm's details
                farmResponses.forEach(farm -> log.info(
                                "Farm: id={}, farmerName={}, location={}, totalArea={} {}, contactNumber={}",
                                farm.getId(), farm.getFarmerName(), farm.getLocation(),
                                farm.getTotalArea(), farm.getAreaUnit(), farm.getContactNumber()));

                log.info("Successfully retrieved {} farms", farmResponses.size());
                return farmResponses;
        }

        @Transactional(readOnly = true)
        public Page<FarmResponse> getFarmsPaged(Pageable pageable) {
                return farmRepository.findAll(pageable)
                                .map(this::mapToFarmResponse);
        }

        @Transactional(readOnly = true)
        public FarmResponse getFarmById(UUID id) {
                Farm farm = farmRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", id));
                return mapToFarmResponse(farm);
        }

        @Transactional(readOnly = true)
        public FarmInspectionResponse getInspectionById(UUID id) {
                FarmInspection inspection = inspectionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Inspection", "id", id));
                return mapToInspectionResponse(inspection);
        }

        @Transactional
        public FarmInspectionResponse createInspection(FarmInspectionRequest request, UUID vendorId) {
                log.info("Creating inspection - farmId: {}, vendorId: {}, estimatedBoxes: {}",
                                request.getFarmId(), vendorId, request.getEstimatedBoxes());

                Farm farm = farmRepository.findById(UUID.fromString(request.getFarmId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", request.getFarmId()));

                User vendor = userRepository.findById(vendorId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", vendorId));

                log.debug("Validating GPS location - farmLat: {}, farmLon: {}, inspectionLat: {}, inspectionLon: {}, accuracy: {}",
                                farm.getLatitude(), farm.getLongitude(), request.getGpsLatitude(),
                                request.getGpsLongitude(), request.getGpsAccuracy());

                // Validate GPS location using enhanced validation
                /*
                 * gpsValidationService.validateGpsLocation(
                 * farm.getLatitude(),
                 * farm.getLongitude(),
                 * request.getGpsLatitude(),
                 * request.getGpsLongitude(),
                 * request.getGpsAccuracy()
                 * );
                 */

                // Validate photo/video requirements (fraud prevention)
                int photoCount = 0;
                int videoCount = 0;
                if (request.getPhotoUrls() != null) {
                        for (String photoUrl : request.getPhotoUrls()) {
                                if (photoUrl.endsWith(".mp4") || photoUrl.endsWith(".mov")) {
                                        videoCount++;
                                } else {
                                        photoCount++;
                                }
                        }
                }
                log.debug("Validating media - photoCount: {}, videoCount: {}", photoCount, videoCount);
                photoValidationService.validateInspectionMedia(photoCount, videoCount);

                // Update farm status
                farm.setStatus(com.banana.harvest.entity.enums.FarmStatus.INSPECTION_PENDING);
                farmRepository.save(farm);

                FarmInspection inspection = FarmInspection.builder()
                                .farm(farm)
                                .vendor(vendor)
                                .estimatedBoxes(request.getEstimatedBoxes())
                                .inspectionNotes(request.getInspectionNotes())
                                .gpsLatitude(request.getGpsLatitude())
                                .gpsLongitude(request.getGpsLongitude())
                                .gpsAccuracy(request.getGpsAccuracy())
                                .status(InspectionStatus.PENDING)
                                .build();

                if (request.getRequestId() != null && !request.getRequestId().isEmpty()) {
                        UUID requestId = UUID.fromString(request.getRequestId());
                        inspection.setRequestId(requestId);

                        inspectionRequestRepository.findById(requestId).ifPresent(req -> {
                                req.setStatus(com.banana.harvest.entity.enums.InspectionRequestStatus.COMPLETED);
                                req.setCompletedAt(LocalDateTime.now());
                                inspectionRequestRepository.save(req);
                                log.info("Linked inspection to request and marked request as COMPLETED - requestId: {}",
                                                requestId);
                        });
                }

                FarmInspection savedInspection = inspectionRepository.save(inspection);

                // Save photos
                if (request.getPhotoUrls() != null) {
                        for (String photoUrl : request.getPhotoUrls()) {
                                FarmPhoto photo = FarmPhoto.builder()
                                                .inspection(savedInspection)
                                                .photoUrl(photoUrl)
                                                .photoType(photoUrl.endsWith(".mp4") ? "VIDEO" : "IMAGE")
                                                .uploadedBy(vendor)
                                                .build();
                                photoRepository.save(photo);
                        }
                }

                log.info("Inspection created successfully - inspectionId: {}, farmId: {}, vendorId: {}, status: {}",
                                savedInspection.getId(), farm.getId(), vendorId, savedInspection.getStatus());

                // Notify managers about new inspection
                notificationService.notifyNewInspection(savedInspection.getId());

                return mapToInspectionResponse(savedInspection);
        }

        @Transactional
        public BatchResponse approveInspection(UUID inspectionId, ApprovalRequest request, UUID approverId) {
                log.info("Processing inspection approval - inspectionId: {}, approved: {}, approverId: {}",
                                inspectionId, request.getApproved(), approverId);

                FarmInspection inspection = inspectionRepository.findById(inspectionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Inspection", "id", inspectionId));

                User approver = userRepository.findById(approverId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", approverId));

                if (request.getApproved()) {
                        inspection.setStatus(InspectionStatus.APPROVED);
                        inspection.setApprovedBy(approver);
                        inspection.setApprovedAt(LocalDateTime.now());
                        inspectionRepository.save(inspection);

                        // Update farm status
                        Farm farm = inspection.getFarm();
                        farm.setStatus(com.banana.harvest.entity.enums.FarmStatus.READY_FOR_HARVEST);
                        farmRepository.save(farm);

                        // Create batch
                        String batchId = generateBatchId();
                        log.debug("Creating batch - batchId: {}, inspectionId: {}, farmId: {}, vendorId: {}",
                                        batchId, inspectionId, inspection.getFarm().getId(),
                                        inspection.getVendor().getId());

                        Batch batch = Batch.builder()
                                        .batchId(batchId)
                                        .inspection(inspection)
                                        .farm(inspection.getFarm())
                                        .vendor(inspection.getVendor())
                                        .status(BatchStatus.CREATED)
                                        .estimatedBoxes(inspection.getEstimatedBoxes())
                                        .allocatedBoxes(inspection.getEstimatedBoxes())
                                        .harvestedBoxes(0)
                                        .remainingBoxes(inspection.getEstimatedBoxes())
                                        .dispatchedBoxes(0)
                                        .gatePassRemaining(0)
                                        .startDate(LocalDate.now())
                                        .createdBy(approver)
                                        .build();

                        Batch savedBatch = batchRepository.save(batch);

                        log.info("Inspection approved and batch created - inspectionId: {}, batchId: {}, estimatedBoxes: {}",
                                        inspectionId, batchId, inspection.getEstimatedBoxes());

                        // Notify vendor about approval
                        notificationService.notifyInspectionApproved(inspection.getId(), batchId);

                        return mapToBatchResponse(savedBatch);
                } else {
                        inspection.setStatus(InspectionStatus.REJECTED);
                        inspection.setApprovedBy(approver);
                        inspection.setApprovedAt(LocalDateTime.now());
                        inspection.setRejectionReason(request.getRejectionReason());
                        inspectionRepository.save(inspection);

                        // Update farm status
                        Farm farm = inspection.getFarm();
                        farm.setStatus(com.banana.harvest.entity.enums.FarmStatus.INSPECTION_REJECTED);
                        farmRepository.save(farm);

                        log.warn("Inspection rejected - inspectionId: {}, reason: {}", inspectionId,
                                        request.getRejectionReason());

                        // Notify vendor about rejection
                        notificationService.notifyInspectionRejected(inspection.getId(), request.getRejectionReason());

                        throw new BusinessException("Inspection rejected: " + request.getRejectionReason());
                }
        }

        @Transactional(readOnly = true)
        public List<FarmInspectionResponse> getAllInspections() {
                return inspectionRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                                .stream()
                                .map(this::mapToInspectionResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<FarmInspectionResponse> getPendingInspections() {
                log.info("Fetching pending inspections from database");
                List<FarmInspection> inspections = inspectionRepository.findByStatus(InspectionStatus.PENDING);
                log.info("Found {} pending inspections in database", inspections.size());

                List<FarmInspectionResponse> responses = inspections.stream()
                                .map(this::mapToInspectionResponse)
                                .collect(Collectors.toList());

                // Log each pending inspection's details
                responses.forEach(inspection -> log.info(
                                "Pending Inspection: id={}, farmName={}, vendorName={}, estimatedBoxes={}, createdAt={}",
                                inspection.getId(), inspection.getFarmName(), inspection.getVendorName(),
                                inspection.getEstimatedBoxes(), inspection.getCreatedAt()));

                log.info("Successfully retrieved {} pending inspections", responses.size());
                return responses;
        }

        @Transactional(readOnly = true)
        public List<FarmInspectionResponse> getVendorInspections(UUID vendorId) {
                log.info("Fetching inspections for vendor: {}", vendorId);
                List<FarmInspection> inspections = inspectionRepository.findByVendorId(vendorId);
                log.info("Found {} inspections for vendor: {}", inspections.size(), vendorId);

                List<FarmInspectionResponse> responses = inspections.stream()
                                .map(this::mapToInspectionResponse)
                                .collect(Collectors.toList());

                // Log each inspection's details
                responses.forEach(inspection -> log.info(
                                "Vendor Inspection: id={}, farmName={}, farmLocation={}, status={}, estimatedBoxes={}, createdAt={}, approvedAt={}",
                                inspection.getId(), inspection.getFarmName(), inspection.getFarmLocation(),
                                inspection.getStatus(), inspection.getEstimatedBoxes(),
                                inspection.getCreatedAt(), inspection.getApprovedAt()));

                // Log summary by status
                long pendingCount = responses.stream().filter(i -> i.getStatus() == InspectionStatus.PENDING || i.getStatus() == InspectionStatus.ASSIGNED || i.getStatus() == InspectionStatus.REQUESTED || i.getStatus() == InspectionStatus.IN_PROGRESS).count();
                long approvedCount = responses.stream().filter(i -> i.getStatus() == InspectionStatus.APPROVED).count();
                long rejectedCount = responses.stream().filter(i -> i.getStatus() == InspectionStatus.REJECTED).count();

                log.info("Vendor {} inspection summary: Total={}, Actionable={}, Approved={}, Rejected={}",
                                vendorId, responses.size(), pendingCount, approvedCount, rejectedCount);

                return responses;
        }

        @Transactional(readOnly = true)
        public long getPendingInspectionCount(UUID vendorId) {
                return inspectionRepository.countPendingByVendor(vendorId);
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

        @Transactional(readOnly = true)
        public BatchResponse getBatchById(UUID id) {
                Batch batch = batchRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", id));
                return mapToBatchResponse(batch);
        }

        private String generateBatchId() {
                String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                long count = batchRepository.count() + 1;
                return "BATCH-" + datePrefix + "-" + String.format("%04d", count);
        }

        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
                final int R = 6371; // Earth's radius in km
                double latDistance = Math.toRadians(lat2 - lat1);
                double lonDistance = Math.toRadians(lon2 - lon1);
                double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                                                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                return R * c;
        }

        private FarmResponse mapToFarmResponse(Farm farm) {
                return FarmResponse.builder()
                                .id(farm.getId())
                                .farmerName(farm.getFarmerName())
                                .location(farm.getLocation())
                                .latitude(farm.getLatitude())
                                .longitude(farm.getLongitude())
                                .contactNumber(farm.getContactNumber())
                                .totalArea(farm.getTotalArea())
                                .areaUnit(farm.getAreaUnit())
                                .produceType(farm.getProduceType())
                                .createdBy(farm.getCreatedBy() != null ? farm.getCreatedBy().getId() : null)
                                .createdByName(farm.getCreatedBy() != null ? farm.getCreatedBy().getFullName() : null)
                                .createdAt(farm.getCreatedAt())
                                .status(farm.getStatus())
                                .build();
        }

        private FarmInspectionResponse mapToInspectionResponse(FarmInspection inspection) {
                return FarmInspectionResponse.builder()
                                .id(inspection.getId())
                                .farmId(inspection.getFarm().getId())
                                .farmName(inspection.getFarm().getFarmerName())
                                .farmLocation(inspection.getFarm().getLocation())
                                .vendorId(inspection.getVendor().getId())
                                .vendorName(inspection.getVendor().getFullName())
                                .estimatedBoxes(inspection.getEstimatedBoxes())
                                .inspectionNotes(inspection.getInspectionNotes())
                                .gpsLatitude(inspection.getGpsLatitude())
                                .gpsLongitude(inspection.getGpsLongitude())
                                .gpsAccuracy(inspection.getGpsAccuracy())
                                .status(inspection.getStatus())
                                .approvedBy(inspection.getApprovedBy() != null ? inspection.getApprovedBy().getId()
                                                : null)
                                .approvedByName(inspection.getApprovedBy() != null
                                                ? inspection.getApprovedBy().getFullName()
                                                : null)
                                .approvedAt(inspection.getApprovedAt())
                                .rejectionReason(inspection.getRejectionReason())
                                .photoUrls(inspection.getPhotos().stream().map(FarmPhoto::getPhotoUrl)
                                                .collect(Collectors.toList()))
                                .requestId(inspection.getRequestId())
                                .createdAt(inspection.getCreatedAt())
                                .build();
        }

        private BatchResponse mapToBatchResponse(Batch batch) {
                return BatchResponse.builder()
                                .id(batch.getId())
                                .batchId(batch.getBatchId())
                                .inspectionId(batch.getInspection() != null ? batch.getInspection().getId() : null)
                                .farmId(batch.getFarm() != null ? batch.getFarm().getId() : null)
                                .farmName(batch.getFarm() != null ? batch.getFarm().getFarmerName() : null)
                                .produceType(batch.getFarm() != null ? batch.getFarm().getProduceType() : null)
                                .vendorId(batch.getVendor() != null ? batch.getVendor().getId() : null)
                                .vendorName(batch.getVendor() != null ? batch.getVendor().getFullName() : null)
                                .status(batch.getStatus())
                                .estimatedBoxes(batch.getEstimatedBoxes())
                                .allocatedBoxes(batch.getAllocatedBoxes())
                                .harvestedBoxes(batch.getHarvestedBoxes())
                                .remainingBoxes(batch.getRemainingBoxes())
                                .actualBoxes(batch.getActualBoxes())
                                .dispatchedBoxes(batch.getDispatchedBoxes())
                                .gatePassRemaining(batch.getGatePassRemaining())
                                .startDate(batch.getStartDate())
                                .endDate(batch.getEndDate())
                                .createdAt(batch.getCreatedAt())
                                .build();
        }

        // Inspection Request methods
        @Transactional
        public InspectionRequestResponse createInspectionRequest(InspectionRequestDto request, UUID createdById) {
                log.info("Creating inspection request - farmId: {}, vendorId: {}, createdBy: {}",
                                request.getFarmId(), request.getVendorId(), createdById);

                Farm farm = farmRepository.findById(UUID.fromString(request.getFarmId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", request.getFarmId()));

                User vendor = userRepository.findById(UUID.fromString(request.getVendorId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id",
                                                request.getVendorId()));

                User createdBy = userRepository.findById(createdById)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdById));

                InspectionRequest inspectionRequest = InspectionRequest.builder()
                                .farm(farm)
                                .vendor(vendor)
                                .createdBy(createdBy)
                                .notes(request.getNotes())
                                .status(InspectionRequestStatus.PENDING)
                                .build();

                InspectionRequest saved = inspectionRequestRepository.save(inspectionRequest);

                log.info("Inspection request created successfully - requestId: {}", saved.getId());

                // Notify vendor
                notificationService.notifyInspectionRequestCreated(saved.getId());

                return mapToInspectionRequestResponse(saved);
        }

        @Transactional(readOnly = true)
        public List<InspectionRequestResponse> getAllInspectionRequests(String status) {
                log.info("Fetching all inspection requests, status: {}", status);

                List<InspectionRequest> requests;
                if (status != null && !status.isEmpty()) {
                        InspectionRequestStatus requestStatus = InspectionRequestStatus.valueOf(status.toUpperCase());
                        requests = inspectionRequestRepository.findByStatus(requestStatus);
                } else {
                        requests = inspectionRequestRepository.findAll();
                }

                return requests.stream()
                                .map(this::mapToInspectionRequestResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<InspectionRequestResponse> getVendorInspectionRequests(UUID vendorId, String status) {
                log.info("Fetching inspection requests for vendor: {}, status: {}", vendorId, status);

                List<InspectionRequest> requests;
                if (status != null && !status.isEmpty()) {
                        InspectionRequestStatus requestStatus = InspectionRequestStatus.valueOf(status.toUpperCase());
                        requests = inspectionRequestRepository.findByVendorIdAndStatus(vendorId, requestStatus);
                } else {
                        requests = inspectionRequestRepository.findByVendorId(vendorId);
                }

                return requests.stream()
                                .map(this::mapToInspectionRequestResponse)
                                .collect(Collectors.toList());
        }

        private InspectionRequestResponse mapToInspectionRequestResponse(InspectionRequest request) {
                return InspectionRequestResponse.builder()
                                .id(request.getId())
                                .farmId(request.getFarm().getId())
                                .farmName(request.getFarm().getFarmerName())
                                .vendorId(request.getVendor().getId())
                                .vendorName(request.getVendor().getFullName())
                                .notes(request.getNotes())
                                .status(request.getStatus())
                                .createdAt(request.getCreatedAt())
                                .completedAt(request.getCompletedAt())
                                .build();
        }
}
