package com.autopro.backend.dto.servicerequest;

import com.autopro.backend.entity.CancellationReason;
import com.autopro.backend.entity.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateServiceRequestStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private ServiceRequestStatus status;

    /** Obligatoire quand {@code status == CANCELLED} : motif choisi dans la liste. */
    private CancellationReason cancellationReason;
}