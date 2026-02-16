package com.banana.harvest.service;

import com.banana.harvest.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    private final PhotoValidationService photoValidationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.storage.bucket}")
    private String bucketName;

    // =========================
    // PHOTO UPLOAD
    // =========================
    public String uploadPhoto(MultipartFile file, UUID userId, UUID inspectionId) {

        log.info("Uploading photo: userId={}, inspectionId={}", userId, inspectionId);

        photoValidationService.validateCameraPhoto(file);
        photoValidationService.validateNotScreenshot(file);

        String fileName = generateFileName(file.getOriginalFilename(), userId, "photos");

        return uploadFile(file, fileName);
    }

    // =========================
    // VIDEO UPLOAD
    // =========================
    public String uploadVideo(MultipartFile file, UUID userId, UUID inspectionId) {

        log.info("Uploading video: userId={}, inspectionId={}", userId, inspectionId);

        photoValidationService.validateCameraVideo(file);

        String fileName = generateFileName(file.getOriginalFilename(), userId, "videos");

        return uploadFile(file, fileName);
    }

    // =========================
    // CORE UPLOAD METHOD
    // =========================
    private String uploadFile(MultipartFile file, String fileName) {

        try {

            String url = supabaseUrl +
                    "/storage/v1/object/" +
                    bucketName + "/" +
                    fileName;

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            headers.set("x-upsert", "false");

            HttpEntity<byte[]> entity =
                    new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()) {

                log.error("Supabase upload failed: {}", response.getBody());

                throw new BusinessException("Supabase upload failed");
            }

            return getPublicUrl(fileName);

        } catch (IOException e) {

            throw new BusinessException("File upload failed: " + e.getMessage());
        }
    }

    // =========================
    // DELETE FILE
    // =========================
    public void deleteFile(String fileUrl) {

        try {

            String fileName = extractFileName(fileUrl);

            String url = supabaseUrl +
                    "/storage/v1/object/" +
                    bucketName + "/" +
                    fileName;

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(supabaseKey);
            headers.set("apikey", supabaseKey);

            HttpEntity<Void> entity =
                    new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.DELETE,
                            entity,
                            String.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()) {

                throw new BusinessException("Delete failed");
            }

        } catch (Exception e) {

            throw new BusinessException("Delete failed: " + e.getMessage());
        }
    }

    // =========================
    // PUBLIC URL
    // =========================
    public String getPublicUrl(String fileName) {

        return supabaseUrl +
                "/storage/v1/object/public/" +
                bucketName +
                "/" +
                fileName;
    }

    // =========================
    // SIGNED URL
    // =========================
    public String generateSignedUrl(String fileName, int expirySeconds) {

        try {

            String url = supabaseUrl +
                    "/storage/v1/object/sign/" +
                    bucketName +
                    "/" +
                    fileName;

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body =
                    "{\"expiresIn\": " + expirySeconds + "}";

            HttpEntity<String> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            JsonNode jsonNode =
                    objectMapper.readTree(response.getBody());

            String signedPath =
                    jsonNode.get("signedURL").asText();

            return supabaseUrl + signedPath;

        } catch (Exception e) {

            throw new BusinessException("Signed URL failed: " + e.getMessage());
        }
    }

    // =========================
    // LIST FILES
    // =========================
    public List<String> listFiles(String folder) {

        try {

            String url =
                    supabaseUrl +
                            "/storage/v1/object/list/" +
                            bucketName +
                            "?prefix=" +
                            folder;

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(supabaseKey);
            headers.set("apikey", supabaseKey);

            HttpEntity<Void> entity =
                    new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

            JsonNode jsonArray =
                    objectMapper.readTree(response.getBody());

            List<String> result = new ArrayList<>();

            for (JsonNode node : jsonArray) {

                result.add(node.get("name").asText());
            }

            return result;

        } catch (Exception e) {

            throw new BusinessException("List files failed");
        }
    }

    // =========================
    // FILENAME GENERATION
    // =========================
    private String generateFileName(
            String original,
            UUID userId,
            String folder
    ) {

        String timestamp =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter
                                        .ofPattern("yyyyMMddHHmmss")
                        );

        String ext =
                original.substring(
                        original.lastIndexOf(".") + 1
                );

        return folder +
                "/" +
                userId +
                "_" +
                timestamp +
                "_" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8) +
                "." +
                ext;
    }

    // =========================
    // EXTRACT NAME
    // =========================
    private String extractFileName(String url) {

        String key =
                "/object/public/" +
                        bucketName +
                        "/";

        int index =
                url.indexOf(key);

        if (index == -1) {

            key =
                    "/object/" +
                            bucketName +
                            "/";

            index =
                    url.indexOf(key);
        }

        if (index == -1)
            throw new BusinessException("Invalid URL");

        return url.substring(index + key.length());
    }
}
