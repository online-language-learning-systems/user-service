package com.hub.user_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import org.aspectj.weaver.ast.Not;

public record UserPostDto (

    @NotBlank
    String username,

    @NotBlank
    String password,

    @NotBlank
    String passwordConfirm,

    @NotBlank
    String email,

    String role,

    @NotBlank
    String lastName,

    @NotBlank
    String firstName

) {

}
