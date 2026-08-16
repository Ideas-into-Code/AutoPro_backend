package com.autopro.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ValidateMechanicRequest {

    @NotNull(message = "approved field is required")
    private Boolean approved;
}
