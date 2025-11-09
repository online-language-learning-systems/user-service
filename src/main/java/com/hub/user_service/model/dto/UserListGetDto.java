package com.hub.user_service.model.dto;

import java.util.List;

public record UserListGetDto(
        List<UserInfoGetDto> userInfoGetDtos
) {
}
