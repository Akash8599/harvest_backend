package com.banana.harvest.service;

import com.banana.harvest.entity.FarmInspection;
import com.banana.harvest.entity.User;
import com.banana.harvest.entity.enums.UserRole;
import com.banana.harvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for sending notifications
 * Supports push notifications, email, and SMS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final com.banana.harvest.repository.FarmInspectionRepository farmInspectionRepository;
    private final com.banana.harvest.repository.InspectionRequestRepository inspectionRequestRepository;

    /**
     * Notifies managers when a new inspection is submitted
     */
    @Async
    @Transactional(readOnly = true)
    public void notifyNewInspection(UUID inspectionId) {
        log.info("Sending notification for new inspection: {}", inspectionId);

        FarmInspection inspection = farmInspectionRepository.findById(inspectionId).orElse(null);
        if (inspection == null)
            return;

        List<User> managers = userRepository.findByRole(UserRole.MANAGER);
        List<User> admins = userRepository.findByRole(UserRole.SUPER_ADMIN);

        String title = "New Farm Inspection Pending Approval";
        String message = String.format("Vendor %s submitted inspection for %s. Estimated boxes: %d",
                inspection.getVendor().getFullName(),
                inspection.getFarm().getFarmerName(),
                inspection.getEstimatedBoxes());

        // Notify managers
        for (User manager : managers) {
            if (manager.getIsActive()) {
                sendPushNotification(manager, title, message, "inspection", inspection.getId().toString());
                sendEmail(manager, title, message);
            }
        }

        // Notify admins
        for (User admin : admins) {
            if (admin.getIsActive()) {
                sendPushNotification(admin, title, message, "inspection", inspection.getId().toString());
            }
        }
    }

    /**
     * Notifies vendor when inspection is approved
     */
    @Async
    @Transactional(readOnly = true)
    public void notifyInspectionApproved(UUID inspectionId, String batchId) {
        log.info("Sending approval notification for inspection: {}", inspectionId);

        FarmInspection inspection = farmInspectionRepository.findById(inspectionId).orElse(null);
        if (inspection == null)
            return;

        log.info("Sending approval notification to vendor: {}", inspection.getVendor().getId());

        String title = "Inspection Approved!";
        String message = String.format("Your inspection for %s has been approved. Batch ID: %s",
                inspection.getFarm().getFarmerName(),
                batchId);

        sendPushNotification(inspection.getVendor(), title, message, "batch", batchId);
        sendEmail(inspection.getVendor(), title, message);
    }

    /**
     * Notifies vendor when inspection is rejected
     */
    @Async
    @Transactional(readOnly = true)
    public void notifyInspectionRejected(UUID inspectionId, String reason) {
        log.info("Sending rejection notification for inspection: {}", inspectionId);

        FarmInspection inspection = farmInspectionRepository.findById(inspectionId).orElse(null);
        if (inspection == null)
            return;

        log.info("Sending rejection notification to vendor: {}", inspection.getVendor().getId());

        String title = "Inspection Rejected";
        String message = String.format("Your inspection for %s was rejected. Reason: %s",
                inspection.getFarm().getFarmerName(),
                reason);

        sendPushNotification(inspection.getVendor(), title, message, "inspection", inspection.getId().toString());
        sendEmail(inspection.getVendor(), title, message);
    }

    /**
     * Notifies vendor when inspection request is created
     */
    @Async
    @Transactional(readOnly = true)
    public void notifyInspectionRequestCreated(UUID requestId) {
        log.info("Sending inspection request notification for request: {}", requestId);

        com.banana.harvest.entity.InspectionRequest request = inspectionRequestRepository.findById(requestId)
                .orElse(null);
        if (request == null)
            return;

        log.info("Sending inspection request notification to vendor: {}", request.getVendor().getId());

        String title = "New Inspection Request";
        String message = String.format("You have been assigned to inspect %s. Please complete the inspection.",
                request.getFarm().getFarmerName());

        sendPushNotification(request.getVendor(), title, message, "inspection-request", request.getId().toString());
        sendEmail(request.getVendor(), title, message);
    }

    /**
     * Notifies store keeper when gate pass is created
     */
    @Async
    public void notifyGatePassCreated(String gatePassNo, Integer totalBoxes, User vendor) {
        log.info("Sending gate pass notification to store keepers");

        List<User> storeKeepers = userRepository.findByRole(UserRole.STORE_KEEPER);

        String title = "New Gate Pass Created";
        String message = String.format("Gate Pass %s created for %d boxes from vendor %s",
                gatePassNo, totalBoxes, vendor.getFullName());

        for (User storeKeeper : storeKeepers) {
            if (storeKeeper.getIsActive()) {
                sendPushNotification(storeKeeper, title, message, "gatepass", gatePassNo);
            }
        }
    }

    /**
     * Notifies manager when stock is low
     */
    @Async
    public void notifyLowStock(String itemName, Integer availableQuantity, Integer threshold) {
        log.info("Sending low stock notification for: {}", itemName);

        List<User> managers = userRepository.findByRole(UserRole.MANAGER);
        List<User> admins = userRepository.findByRole(UserRole.SUPER_ADMIN);

        String title = "Low Stock Alert";
        String message = String.format("%s stock is low. Available: %d (Threshold: %d)",
                itemName, availableQuantity, threshold);

        for (User user : managers) {
            if (user.getIsActive()) {
                sendPushNotification(user, title, message, "inventory", null);
            }
        }

        for (User user : admins) {
            if (user.getIsActive()) {
                sendPushNotification(user, title, message, "inventory", null);
            }
        }
    }

    /**
     * Notifies vendor when materials are allocated
     */
    @Async
    public void notifyMaterialsAllocated(User vendor, String batchId, String materials) {
        log.info("Sending material allocation notification to vendor: {}", vendor.getId());

        String title = "Materials Allocated";
        String message = String.format("Materials allocated for Batch %s: %s",
                batchId, materials);

        sendPushNotification(vendor, title, message, "batch", batchId);
        sendEmail(vendor, title, message);
    }

    /**
     * Notifies admin when sale is created
     */
    @Async
    public void notifySaleCreated(String invoiceNumber, String buyerName, String batchId, String amount) {
        log.info("Sending sale notification to admins");

        List<User> admins = userRepository.findByRole(UserRole.SUPER_ADMIN);
        List<User> managers = userRepository.findByRole(UserRole.MANAGER);

        String title = "New Sale Created";
        String message = String.format("Invoice %s created for %s. Amount: %s",
                invoiceNumber, buyerName, amount);

        for (User user : admins) {
            if (user.getIsActive()) {
                sendPushNotification(user, title, message, "sale", invoiceNumber);
            }
        }

        for (User user : managers) {
            if (user.getIsActive()) {
                sendPushNotification(user, title, message, "sale", invoiceNumber);
            }
        }
    }

    /**
     * Sends push notification
     */
    private void sendPushNotification(User user, String title, String message, String type, String referenceId) {
        // In production, integrate with Firebase Cloud Messaging (FCM)
        // or OneSignal for push notifications
        log.info("[PUSH] To: {}, Title: {}, Message: {}", user.getEmail(), title, message);

        // Example FCM integration:
        // Message fcmMessage = Message.builder()
        // .setToken(user.getFcmToken())
        // .setNotification(Notification.builder()
        // .setTitle(title)
        // .setBody(message)
        // .build())
        // .putData("type", type)
        // .putData("referenceId", referenceId)
        // .build();
        // FirebaseMessaging.getInstance().send(fcmMessage);
    }

    /**
     * Sends email notification
     */
    private void sendEmail(User user, String subject, String body) {
        // In production, integrate with SendGrid, AWS SES, or similar
        log.info("[EMAIL] To: {}, Subject: {}", user.getEmail(), subject);

        // Example email integration would go here
    }

    /**
     * Sends SMS notification
     */
    private void sendSms(User user, String message) {
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            return;
        }

        // In production, integrate with Twilio or similar
        log.info("[SMS] To: {}, Message: {}", user.getPhone(), message);

        // Example SMS integration would go here
    }
}
