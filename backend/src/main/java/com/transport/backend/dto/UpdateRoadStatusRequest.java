package com.transport.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoadStatusRequest(
        @NotBlank(message = "fromStopId must not be blank")
        String fromStopId,

        @NotBlank(message = "toStopId must not be blank")
        String toStopId,

        boolean open,

        Boolean bidirectional
) {
    public boolean isBidirectional() {
        return bidirectional != null && bidirectional;
    }
}
