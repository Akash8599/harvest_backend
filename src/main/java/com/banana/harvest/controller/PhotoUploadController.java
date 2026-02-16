package com.banana.harvest.controller;

import com.banana.harvest.dto.common.ApiResponse;
import com.banana.harvest.security.UserPrincipal;
import com.banana.harvest.service.PhotoValidationService;
import com.banana.harvest.service.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller for photo and video uploads
 * Enforces camera-only uploads for fraud prevention
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "Photo Upload", description = "Camera-only photo/video upload APIs with fraud prevention")
public class PhotoUploadController {

    private final SupabaseStorageService supabaseStorageService;
    private final PhotoValidationService photoValidationService;

    /**
     * Uploads a single photo from camera
     */
    @PostMapping("/photo")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Upload photo from camera", description = "Upload a photo taken directly from camera. Gallery uploads are rejected.")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String inspectionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Photo upload request from user: {}", userPrincipal.getId());

        String photoUrl = supabaseStorageService.uploadPhoto(
                file, 
                userPrincipal.getId(), 
                inspectionId != null ? UUID.fromString(inspectionId) : null
        );

        return ResponseEntity.ok(ApiResponse.success("Photo uploaded successfully", photoUrl));
    }

    /**
     * Uploads multiple photos from camera
     */
    @PostMapping("/photos")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Upload multiple photos", description = "Upload multiple photos taken directly from camera")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultiplePhotos(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String inspectionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Multiple photo upload request: {} files from user: {}", files.size(), userPrincipal.getId());

        // Validate count
        photoValidationService.validateInspectionMedia(files.size(), 0);

        List<String> photoUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = supabaseStorageService.uploadPhoto(
                    file, 
                    userPrincipal.getId(), 
                    inspectionId != null ? UUID.fromString(inspectionId) : null
            );
            photoUrls.add(url);
        }

        return ResponseEntity.ok(ApiResponse.success("Photos uploaded successfully", photoUrls));
    }

    /**
     * Uploads a video from camera
     */
    @PostMapping("/video")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Upload video from camera", description = "Upload a video taken directly from camera")
    public ResponseEntity<ApiResponse<String>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String inspectionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Video upload request from user: {}", userPrincipal.getId());

        String videoUrl = supabaseStorageService.uploadVideo(
                file, 
                userPrincipal.getId(), 
                inspectionId != null ? UUID.fromString(inspectionId) : null
        );

        return ResponseEntity.ok(ApiResponse.success("Video uploaded successfully", videoUrl));
    }

    /**
     * Uploads complete inspection media (photos + video)
     */
    @PostMapping("/inspection-media")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Upload inspection media", description = "Upload 4-5 photos and 1 video for inspection")
    public ResponseEntity<ApiResponse<InspectionMediaResponse>> uploadInspectionMedia(
            @RequestParam("photos") List<MultipartFile> photos,
            @RequestParam("video") MultipartFile video,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Inspection media upload: {} photos, 1 video from user: {}", photos.size(), userPrincipal.getId());

        // Validate media count
        photoValidationService.validateInspectionMedia(photos.size(), 1);

        // Upload photos
        List<String> photoUrls = new ArrayList<>();
        for (MultipartFile photo : photos) {
            String url = supabaseStorageService.uploadPhoto(photo, userPrincipal.getId(), null);
            photoUrls.add(url);
        }

        // Upload video
        String videoUrl = supabaseStorageService.uploadVideo(video, userPrincipal.getId(), null);

        InspectionMediaResponse response = new InspectionMediaResponse(photoUrls, videoUrl);

        return ResponseEntity.ok(ApiResponse.success("Inspection media uploaded successfully", response));
    }

    /**
     * Deletes a file from Supabase Storage
     */
    @DeleteMapping("/file")
    @PreAuthorize("hasRole('VENDOR') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Delete uploaded file", description = "Delete a previously uploaded photo or video")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam String fileUrl) {
        supabaseStorageService.deleteFile(fileUrl);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    // Response DTO
    public record InspectionMediaResponse(List<String> photoUrls, String videoUrl) {}
}
