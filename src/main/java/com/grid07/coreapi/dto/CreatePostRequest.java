package com.grid07.coreapi.dto;

import com.grid07.coreapi.model.ActorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull ActorType authorType,
        @NotNull @Positive Long authorId,
        @NotBlank @Size(max = 5000) String content
) {
}
