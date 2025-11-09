package com.hub.user_service.model.dto;

public record UserInfoGetDto(
        String userId,
        String username,
        String email,
        String firstName,
        String lastName
) {
}
