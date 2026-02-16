package com.banana.harvest.service;

import com.banana.harvest.entity.Farm;
import com.banana.harvest.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for GPS validation and fraud prevention
 * Ensures users are actually at the farm location
 */
@Slf4j
@Service
public class GpsValidationService {

    // Maximum distance allowed from farm location (in kilometers)
    private static final double MAX_DISTANCE_KM = 0.5; // 500 meters
    
    // Minimum GPS accuracy required (in meters)
    private static final double MIN_ACCURACY_METERS = 50.0;
    
    // Maximum GPS accuracy allowed (in meters) - too inaccurate is suspicious
    private static final double MAX_ACCURACY_METERS = 100.0;
    
    // Earth's radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Validates GPS coordinates against farm location
     * Checks distance, accuracy, and potential spoofing
     */
    public void validateGpsLocation(
            BigDecimal farmLat, 
            BigDecimal farmLng,
            BigDecimal gpsLat, 
            BigDecimal gpsLng,
            BigDecimal gpsAccuracy) {
        
        // Validate coordinates are not null
        if (gpsLat == null || gpsLng == null) {
            throw new BusinessException("GPS coordinates are required", "GPS_MISSING");
        }

        // Validate coordinate ranges
        validateCoordinateRanges(gpsLat, gpsLng);

        // Check GPS accuracy
        if (gpsAccuracy != null) {
            validateGpsAccuracy(gpsAccuracy.doubleValue());
        }

        // Calculate distance from farm
        double distance = calculateDistance(
            farmLat.doubleValue(), 
            farmLng.doubleValue(),
            gpsLat.doubleValue(), 
            gpsLng.doubleValue()
        );

        log.info("GPS Validation - Farm: ({}, {}), GPS: ({}, {}), Distance: {} km, Accuracy: {} m",
            farmLat, farmLng, gpsLat, gpsLng, 
            String.format("%.3f", distance),
            gpsAccuracy != null ? gpsAccuracy : "N/A");

        // Check if within acceptable range
        if (distance > MAX_DISTANCE_KM) {
            throw new BusinessException(
                String.format("You are too far from the farm location. Distance: %.2f meters (Max allowed: %.0f meters)", 
                    distance * 1000, MAX_DISTANCE_KM * 1000),
                "GPS_TOO_FAR"
            );
        }

        // Check for suspicious coordinates (possible GPS spoofing)
        if (isSuspiciousLocation(gpsLat.doubleValue(), gpsLng.doubleValue())) {
            log.warn("Suspicious GPS location detected: ({}, {})", gpsLat, gpsLng);
            throw new BusinessException("Invalid GPS location detected", "GPS_SUSPICIOUS");
        }
    }

    /**
     * Validates GPS accuracy
     */
    private void validateGpsAccuracy(double accuracy) {
        if (accuracy > MAX_ACCURACY_METERS) {
            throw new BusinessException(
                String.format("GPS accuracy is too low (%.0f meters). Please ensure you have a clear view of the sky.", accuracy),
                "GPS_LOW_ACCURACY"
            );
        }
        
        if (accuracy < 0) {
            throw new BusinessException("Invalid GPS accuracy value", "GPS_INVALID_ACCURACY");
        }
    }

    /**
     * Validates coordinate ranges
     */
    private void validateCoordinateRanges(BigDecimal lat, BigDecimal lng) {
        double latitude = lat.doubleValue();
        double longitude = lng.doubleValue();

        if (latitude < -90 || latitude > 90) {
            throw new BusinessException("Invalid latitude value", "INVALID_LATITUDE");
        }

        if (longitude < -180 || longitude > 180) {
            throw new BusinessException("Invalid longitude value", "INVALID_LONGITUDE");
        }

        // Check for null island (0,0) - common GPS error
        if (latitude == 0.0 && longitude == 0.0) {
            throw new BusinessException("Invalid GPS coordinates detected", "GPS_NULL_ISLAND");
        }
    }

    /**
     * Calculates distance between two coordinates using Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Checks if location is suspicious (possible GPS spoofing)
     */
    private boolean isSuspiciousLocation(double lat, double lng) {
        // Check for common mock locations
        // Apple HQ coordinates (common mock location)
        if (isNearCoordinate(lat, lng, 37.3318, -122.0312, 0.1)) {
            return true;
        }
        
        // Google HQ coordinates (common mock location)
        if (isNearCoordinate(lat, lng, 37.4220, -122.0841, 0.1)) {
            return true;
        }
        
        // Check for coordinates in ocean (likely spoofed)
        // Simplified check - in production, use a more comprehensive approach
        
        return false;
    }

    /**
     * Checks if coordinates are near a specific point
     */
    private boolean isNearCoordinate(double lat, double lng, double targetLat, double targetLng, double thresholdKm) {
        double distance = calculateDistance(lat, lng, targetLat, targetLng);
        return distance < thresholdKm;
    }

    /**
     * Validates that GPS timestamp is recent (prevents using old location data)
     */
    public void validateGpsTimestamp(long gpsTimestamp, long maxAgeMinutes) {
        long currentTime = System.currentTimeMillis();
        long ageMinutes = (currentTime - gpsTimestamp) / (1000 * 60);
        
        if (ageMinutes > maxAgeMinutes) {
            throw new BusinessException(
                String.format("GPS location is too old (%d minutes). Please refresh your location.", ageMinutes),
                "GPS_STALE"
            );
        }
    }

    /**
     * Gets human-readable distance string
     */
    public String getDistanceString(double distanceKm) {
        if (distanceKm < 1.0) {
            return String.format("%.0f meters", distanceKm * 1000);
        } else {
            return String.format("%.2f km", distanceKm);
        }
    }
}
