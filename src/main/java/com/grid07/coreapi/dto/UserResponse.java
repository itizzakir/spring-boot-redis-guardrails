package com.grid07.coreapi.dto;

import com.grid07.coreapi.model.AppUser;

public record UserResponse(
        Long id,
        String username,
        boolean premium
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.isPremium());
    }
}
