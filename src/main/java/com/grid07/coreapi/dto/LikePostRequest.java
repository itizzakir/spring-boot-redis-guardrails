package com.grid07.coreapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LikePostRequest(
        @NotNull @Positive Long userId
) {
}
