package com.autopro.backend.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatsDTO {

    private long totalUsers;
    private long totalMechanics;
    private long pendingMechanics;
    private long approvedMechanics;
    private long rejectedMechanics;
    private long activeUsers;
    private long inactiveUsers;
}
