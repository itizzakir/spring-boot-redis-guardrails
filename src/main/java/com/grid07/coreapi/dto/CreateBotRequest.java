package com.grid07.coreapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBotRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 2000) String personaDescription
) {
}
