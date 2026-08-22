package com.autopro.backend.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateDTO {

    private Long mechanicId;
    private Double latitude;
    private Double longitude;
    private Double heading;
    private Instant timestamp;
}
