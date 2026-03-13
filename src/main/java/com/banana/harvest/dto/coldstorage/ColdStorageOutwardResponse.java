package com.banana.harvest.dto.coldstorage;

import com.banana.harvest.entity.enums.Destination;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ColdStorageOutwardResponse {
    private UUID id;
    private String containerNumber;
    private Destination destination;
    private LocalDate dispatchDate;
    private Integer kg13Boxes;
    private Integer kg13_5Boxes;
    private Integer kg7Boxes;
    private Integer kg16Boxes;
    private Integer totalBoxes;
    private String remarks;
    private String createdByName;
    private LocalDateTime createdAt;
}
