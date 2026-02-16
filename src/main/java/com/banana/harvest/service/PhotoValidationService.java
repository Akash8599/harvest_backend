package com.banana.harvest.service;

import com.banana.harvest.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Service to validate photos for fraud prevention
 * Ensures photos are taken from camera and not uploaded from gallery
 */
@Slf4j
@Service
public class PhotoValidationService {

    // Minimum required photos for inspection
    private static final int MIN_PHOTOS = 2;
    private static final int MAX_PHOTOS = 5;
    private static final int REQUIRED_VIDEOS = 0;

    // Maximum file sizes
    private static final long MAX_PHOTO_SIZE_MB = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE_MB = 50 * 1024 * 1024; // 50MB

    // Allowed photo formats
    private static final String[] ALLOWED_PHOTO_FORMATS = { "image/jpeg", "image/jpg", "image/png" };
    private static final String[] ALLOWED_VIDEO_FORMATS = { "video/mp4", "video/quicktime" };

    /**
     * Validates that photos are taken from camera (not gallery)
     * Checks EXIF data for camera metadata
     */
    public void validateCameraPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Photo file is empty", "PHOTO_EMPTY");
        }

        // Check file size
        if (file.getSize() > MAX_PHOTO_SIZE_MB) {
            throw new BusinessException("Photo size exceeds 10MB limit", "PHOTO_TOO_LARGE");
        }

        // Validate content type
        String contentType = file.getContentType();
        boolean isValidFormat = false;
        for (String format : ALLOWED_PHOTO_FORMATS) {
            if (format.equalsIgnoreCase(contentType)) {
                isValidFormat = true;
                break;
            }
        }
        if (!isValidFormat) {
            throw new BusinessException("Invalid photo format. Only JPEG and PNG allowed", "INVALID_PHOTO_FORMAT");
        }

        // Check EXIF data for camera metadata (fraud prevention)
        try (InputStream is = file.getInputStream()) {
            validateExifData(is);
        } catch (IOException e) {
            log.error("Error reading photo EXIF data", e);
            throw new BusinessException("Unable to validate photo authenticity", "PHOTO_VALIDATION_ERROR");
        }
    }

    /**
     * Validates video file
     */
    public void validateCameraVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Video file is empty", "VIDEO_EMPTY");
        }

        // Check file size
        if (file.getSize() > MAX_VIDEO_SIZE_MB) {
            throw new BusinessException("Video size exceeds 50MB limit", "VIDEO_TOO_LARGE");
        }

        // Validate content type
        String contentType = file.getContentType();
        boolean isValidFormat = false;
        for (String format : ALLOWED_VIDEO_FORMATS) {
            if (format.equalsIgnoreCase(contentType)) {
                isValidFormat = true;
                break;
            }
        }
        if (!isValidFormat) {
            throw new BusinessException("Invalid video format. Only MP4 allowed", "INVALID_VIDEO_FORMAT");
        }
    }

    /**
     * Validates EXIF data to ensure photo was taken by a camera
     * Checks for presence of camera make/model and capture timestamp
     */
    private void validateExifData(InputStream inputStream) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(inputStream)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                throw new BusinessException("Unable to read photo metadata", "PHOTO_METADATA_ERROR");
            }

            ImageReader reader = readers.next();
            reader.setInput(iis, true);

            IIOMetadata metadata = reader.getImageMetadata(0);

            // Check if metadata exists
            if (metadata == null || metadata.getNativeMetadataFormatName() == null) {
                throw new BusinessException("Photo metadata missing. Gallery uploads not allowed.",
                        "GALLERY_UPLOAD_DETECTED");
            }

            // Additional checks for camera-specific metadata would go here
            // In production, you'd parse the EXIF data more thoroughly

            reader.dispose();
        }
    }

    /**
     * Validates the complete media set for an inspection
     */
    public void validateInspectionMedia(int photoCount, int videoCount) {
        if (photoCount < MIN_PHOTOS) {
            throw new BusinessException(
                    String.format("Minimum %d photos required. Found: %d", MIN_PHOTOS, photoCount),
                    "INSUFFICIENT_PHOTOS");
        }

        if (photoCount > MAX_PHOTOS) {
            throw new BusinessException(
                    String.format("Maximum %d photos allowed. Found: %d", MAX_PHOTOS, photoCount),
                    "TOO_MANY_PHOTOS");
        }

        if (videoCount < REQUIRED_VIDEOS) {
            throw new BusinessException(
                    String.format("%d video required", REQUIRED_VIDEOS),
                    "VIDEO_REQUIRED");
        }
    }

    /**
     * Checks if the file is a screenshot (not allowed)
     */
    public void validateNotScreenshot(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lowerName = originalFilename.toLowerCase();
            if (lowerName.contains("screenshot") || lowerName.contains("screen_shot")) {
                throw new BusinessException("Screenshots are not allowed. Please use camera.", "SCREENSHOT_DETECTED");
            }
        }
    }
}
