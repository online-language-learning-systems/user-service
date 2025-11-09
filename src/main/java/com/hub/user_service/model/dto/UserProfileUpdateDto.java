package com.hub.user_service.model.dto;

public record UserProfileUpdateDto (
        String firstName, String lastName, String email
) { }
