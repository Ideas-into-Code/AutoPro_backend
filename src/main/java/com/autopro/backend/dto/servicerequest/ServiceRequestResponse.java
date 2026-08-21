package com.autopro.backend.dto.servicerequest;

import com.autopro.backend.entity.ServiceRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ServiceRequestResponse {

    private Long id;

    private Long clientId;
    private String clientName;

    private Long mechanicId;
    private String mechanicName;

    private Long vehicleId;
    private String vehicleLabel;

    private String description;
    private ServiceRequestStatus status;

    private String address;
    private Double latitude;
    private Double longitude;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}