// package com.banana.harvest.service;

// import com.banana.harvest.exception.BusinessException;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.web.multipart.MultipartFile;
// import software.amazon.awssdk.core.sync.RequestBody;
// import software.amazon.awssdk.services.s3.S3Client;
// import software.amazon.awssdk.services.s3.model.*;
// import software.amazon.awssdk.services.s3.presigner.S3Presigner;
// import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

// import java.io.IOException;
// import java.time.Duration;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.UUID;

// /**
//  * Service for AWS S3 file storage operations
//  */
// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class S3StorageService {

//     private final S3Client s3Client;
//     private final S3Presigner s3Presigner;
//     private final PhotoValidationService photoValidationService;

//     @Value("${aws.s3.bucket}")
//     private String bucketName;

//     @Value("${aws.s3.region}")
//     private String region;

//     // Allowed content types
//     private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/jpg", "image/png"};
//     private static final String[] ALLOWED_VIDEO_TYPES = {"video/mp4", "video/quicktime"};

//     /**
//      * Uploads a photo to S3 with validation
//      */
//     public String uploadPhoto(MultipartFile file, UUID userId, UUID inspectionId) {
//         // Validate photo (fraud prevention)
//         photoValidationService.validateCameraPhoto(file);
//         photoValidationService.validateNotScreenshot(file);

//         String fileName = generateFileName(file.getOriginalFilename(), userId, "photos");
//         String contentType = file.getContentType();

//         try {
//             PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                     .bucket(bucketName)
//                     .key(fileName)
//                     .contentType(contentType)
//                     .contentLength(file.getSize())
//                     .metadata(java.util.Map.of(
//                             "userId", userId.toString(),
//                             "inspectionId", inspectionId != null ? inspectionId.toString() : "",
//                             "uploadedAt", LocalDateTime.now().toString()
//                     ))
//                     .build();

//             s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

//             String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
//             log.info("Photo uploaded successfully: {}", fileUrl);

//             return fileUrl;

//         } catch (IOException e) {
//             log.error("Error uploading photo to S3", e);
//             throw new BusinessException("Failed to upload photo: " + e.getMessage());
//         }
//     }

//     /**
//      * Uploads a video to S3 with validation
//      */
//     public String uploadVideo(MultipartFile file, UUID userId, UUID inspectionId) {
//         // Validate video
//         photoValidationService.validateCameraVideo(file);

//         String fileName = generateFileName(file.getOriginalFilename(), userId, "videos");
//         String contentType = file.getContentType();

//         try {
//             PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                     .bucket(bucketName)
//                     .key(fileName)
//                     .contentType(contentType)
//                     .contentLength(file.getSize())
//                     .metadata(java.util.Map.of(
//                             "userId", userId.toString(),
//                             "inspectionId", inspectionId != null ? inspectionId.toString() : "",
//                             "uploadedAt", LocalDateTime.now().toString()
//                     ))
//                     .build();

//             s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

//             String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
//             log.info("Video uploaded successfully: {}", fileUrl);

//             return fileUrl;

//         } catch (IOException e) {
//             log.error("Error uploading video to S3", e);
//             throw new BusinessException("Failed to upload video: " + e.getMessage());
//         }
//     }

//     /**
//      * Generates a presigned URL for temporary access
//      */
//     public String generatePresignedUrl(String fileKey, Duration expiration) {
//         GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                 .bucket(bucketName)
//                 .key(fileKey)
//                 .build();

//         GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                 .signatureDuration(expiration)
//                 .getObjectRequest(getObjectRequest)
//                 .build();

//         return s3Presigner.presignGetObject(presignRequest).url().toString();
//     }

//     /**
//      * Deletes a file from S3
//      */
//     public void deleteFile(String fileUrl) {
//         try {
//             String fileKey = extractFileKeyFromUrl(fileUrl);
            
//             DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
//                     .bucket(bucketName)
//                     .key(fileKey)
//                     .build();

//             s3Client.deleteObject(deleteObjectRequest);
//             log.info("File deleted from S3: {}", fileKey);

//         } catch (Exception e) {
//             log.error("Error deleting file from S3", e);
//             throw new BusinessException("Failed to delete file: " + e.getMessage());
//         }
//     }

//     /**
//      * Generates a unique file name
//      */
//     private String generateFileName(String originalFilename, UUID userId, String folder) {
//         String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//         String extension = getFileExtension(originalFilename);
//         String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
//         return String.format("%s/%s_%s_%s.%s", folder, userId, timestamp, uniqueId, extension);
//     }

//     /**
//      * Extracts file key from S3 URL
//      */
//     private String extractFileKeyFromUrl(String fileUrl) {
//         // Extract key from URL like https://bucket.s3.region.amazonaws.com/key
//         int keyStart = fileUrl.indexOf(".amazonaws.com/");
//         if (keyStart == -1) {
//             throw new BusinessException("Invalid S3 URL");
//         }
//         return fileUrl.substring(keyStart + ".amazonaws.com/".length());
//     }

//     /**
//      * Gets file extension
//      */
//     private String getFileExtension(String filename) {
//         if (filename == null || filename.lastIndexOf(".") == -1) {
//             return "jpg";
//         }
//         return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
//     }
// }
