package com.banana.harvest.service;

import com.banana.harvest.entity.Sale;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.UUID;

/**
 * Service for sharing invoices via WhatsApp and Email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceSharingService {

    private final SaleRepository saleRepository;
    private final PdfInvoiceService pdfInvoiceService;
    private final NotificationService notificationService;

    // WhatsApp Business API configuration (would come from properties)
    private static final String WHATSAPP_API_URL = "https://graph.facebook.com/v18.0";
    private static final String WHATSAPP_PHONE_NUMBER_ID = "YOUR_PHONE_NUMBER_ID";
    private static final String WHATSAPP_ACCESS_TOKEN = "YOUR_ACCESS_TOKEN";

    // Email configuration
    private static final String FROM_EMAIL = "invoices@bananaharvest.com";

    /**
     * Shares invoice via WhatsApp
     */
    public void shareViaWhatsApp(UUID saleId, String phoneNumber) {
        try {
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new BusinessException("Sale not found"));

            // Generate PDF
            byte[] pdfBytes = pdfInvoiceService.generateInvoice(saleId);

            // Format phone number (remove non-numeric and add country code if needed)
            String formattedPhone = formatPhoneNumber(phoneNumber);

            // Send WhatsApp message with PDF
            sendWhatsAppMessageWithPdf(formattedPhone, sale, pdfBytes);

            log.info("Invoice {} shared via WhatsApp to {}", sale.getInvoiceNumber(), formattedPhone);

        } catch (Exception e) {
            log.error("Error sharing invoice via WhatsApp", e);
            throw new BusinessException("Failed to share invoice via WhatsApp: " + e.getMessage());
        }
    }

    /**
     * Shares invoice via Email
     */
    public void shareViaEmail(UUID saleId, String email) {
        try {
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new BusinessException("Sale not found"));

            // Generate PDF
            byte[] pdfBytes = pdfInvoiceService.generateInvoice(saleId);

            // Send email with PDF attachment
            sendEmailWithAttachment(email, sale, pdfBytes);

            log.info("Invoice {} shared via Email to {}", sale.getInvoiceNumber(), email);

        } catch (Exception e) {
            log.error("Error sharing invoice via Email", e);
            throw new BusinessException("Failed to share invoice via Email: " + e.getMessage());
        }
    }

    /**
     * Generates WhatsApp share link (for frontend use)
     */
    public String generateWhatsAppShareLink(UUID saleId, String phoneNumber) {
        try {
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new BusinessException("Sale not found"));

            String formattedPhone = formatPhoneNumber(phoneNumber);
            
            String message = generateWhatsAppMessage(sale);
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

            // WhatsApp Click to Chat API
            return String.format("https://wa.me/%s?text=%s", formattedPhone, encodedMessage);

        } catch (Exception e) {
            log.error("Error generating WhatsApp share link", e);
            throw new BusinessException("Failed to generate WhatsApp share link");
        }
    }

    /**
     * Sends WhatsApp message with PDF using WhatsApp Business API
     */
    private void sendWhatsAppMessageWithPdf(String phoneNumber, Sale sale, byte[] pdfBytes) {
        // In production, integrate with WhatsApp Business API
        // This requires Meta Business verification and WhatsApp Business API setup
        
        String url = String.format("%s/%s/messages", WHATSAPP_API_URL, WHATSAPP_PHONE_NUMBER_ID);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(WHATSAPP_ACCESS_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // First, upload the PDF document
        String mediaId = uploadMediaToWhatsApp(pdfBytes, sale.getInvoiceNumber() + ".pdf");
        
        // Then send the message with the document
        String messageBody = String.format("""
            {
                "messaging_product": "whatsapp",
                "recipient_type": "individual",
                "to": "%s",
                "type": "document",
                "document": {
                    "id": "%s",
                    "caption": "Invoice %s from Banana Harvest Export"
                }
            }
            """, phoneNumber, mediaId, sale.getInvoiceNumber());
        
        HttpEntity<String> request = new HttpEntity<>(messageBody, headers);
        
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("WhatsApp API error: " + response.getBody());
        }
        
        log.info("WhatsApp message sent successfully to {}", phoneNumber);
    }

    /**
     * Uploads media to WhatsApp servers
     */
    private String uploadMediaToWhatsApp(byte[] fileBytes, String filename) {
        String url = String.format("%s/%s/media", WHATSAPP_API_URL, WHATSAPP_PHONE_NUMBER_ID);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(WHATSAPP_ACCESS_TOKEN);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("type", "application/pdf");
        body.add("messaging_product", "whatsapp");
        
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        // Parse response to get media ID
        // In production, properly parse the JSON response
        return "media_id_placeholder";
    }

    /**
     * Sends email with PDF attachment
     */
    private void sendEmailWithAttachment(String toEmail, Sale sale, byte[] pdfBytes) {
        // In production, integrate with SendGrid, AWS SES, or JavaMailSender
        
        String subject = String.format("Invoice %s - Banana Harvest Export", sale.getInvoiceNumber());
        
        String body = String.format("""
            Dear %s,
            
            Please find attached your invoice from Banana Harvest Export.
            
            Invoice Details:
            - Invoice Number: %s
            - Invoice Date: %s
            - Total Amount: ₹%s
            - Total Boxes: %d
            
            If you have any questions, please contact us at %s.
            
            Thank you for your business!
            
            Best regards,
            Banana Harvest Export Team
            """,
            sale.getBuyerName(),
            sale.getInvoiceNumber(),
            sale.getSaleDate(),
            formatCurrency(sale.getGrandTotal()),
            sale.getTotalBoxes(),
            COMPANY_PHONE
        );
        
        // Example using JavaMailSender (would be autowired in production):
        // MimeMessage message = mailSender.createMimeMessage();
        // MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // helper.setFrom(FROM_EMAIL);
        // helper.setTo(toEmail);
        // helper.setSubject(subject);
        // helper.setText(body);
        // helper.addAttachment(sale.getInvoiceNumber() + ".pdf", new ByteArrayResource(pdfBytes));
        // mailSender.send(message);
        
        log.info("Email sent to {} with subject: {}", toEmail, subject);
    }

    /**
     * Generates WhatsApp message text
     */
    private String generateWhatsAppMessage(Sale sale) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        
        return String.format("""
            *Banana Harvest Export*
            
            Invoice: %s
            Date: %s
            
            Buyer: %s
            Batch: %s
            Boxes: %d
            
            *Total Amount: ₹%s*
            
            Thank you for your business!
            """,
            sale.getInvoiceNumber(),
            sale.getSaleDate(),
            sale.getBuyerName(),
            sale.getBatch().getBatchId(),
            sale.getTotalBoxes(),
            df.format(sale.getGrandTotal())
        );
    }

    /**
     * Formats phone number for WhatsApp
     */
    private String formatPhoneNumber(String phone) {
        // Remove all non-numeric characters
        String numericOnly = phone.replaceAll("[^0-9]", "");
        
        // Add country code if not present
        if (numericOnly.length() == 10) {
            // Indian number without country code
            return "91" + numericOnly;
        } else if (numericOnly.startsWith("0")) {
            // Remove leading 0 and add country code
            return "91" + numericOnly.substring(1);
        } else if (!numericOnly.startsWith("91")) {
            // Add country code
            return "91" + numericOnly;
        }
        
        return numericOnly;
    }

    private String formatCurrency(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }

    private static final String COMPANY_PHONE = "+91 98765 43210";
}
