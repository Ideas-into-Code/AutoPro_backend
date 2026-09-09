package com.autopro.backend.dto.servicerequest;

import com.autopro.backend.dto.payment.PaymentResponse;
import com.autopro.backend.entity.CancellationReason;
import com.autopro.backend.entity.ProblemType;
import com.autopro.backend.entity.ServiceRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServiceRequestResponse {

    private Long id;

    private Long clientId;
    private String clientName;

    private Long mechanicId;
    private Long mechanicUserId;
    private String mechanicName;

    private Long vehicleId;
    private String vehicleLabel;

    private String description;
    private ProblemType problemType;
    private String contactPhone;
    private Boolean isEmergency;
    private ServiceRequestStatus status;

    /** Prix convenu, {@code null} tant que le mécanicien ne l'a pas fixé. */
    private BigDecimal price;

    /** Paiement associé, {@code null} tant que la demande n'est pas terminée. */
    private PaymentResponse payment;

    private String address;
    private Double latitude;
    private Double longitude;

    /** Photos jointes par le client. */
    private List<String> photoUrls;

    /** Motif d'annulation, {@code null} sauf si la demande est annulée. */
    private CancellationReason cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}