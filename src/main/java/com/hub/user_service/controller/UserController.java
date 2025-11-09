package com.hub.user_service.controller;

import com.hub.user_service.model.dto.UserListGetDto;
import com.hub.user_service.model.dto.UserPostDto;
import com.hub.user_service.model.dto.UserProfileUpdateDto;
import com.hub.user_service.service.UserService;
import com.hub.user_service.model.dto.UserDetailGetDto;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/backoffice/users")
    public ResponseEntity<UserListGetDto> getAllUserByRole(@RequestParam(name = "role", defaultValue = "student") String role) {
        return ResponseEntity.ok(userService.getAllUserByRole(role));
    }

    @GetMapping("/storefront/user/profile")
    public ResponseEntity<UserDetailGetDto> getUserProfile() {
        // (OIDC) - SecurityContextHolder.getContext().getAuthentication().getName()
        // return the username of the current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info(username);
        return ResponseEntity.ok(userService.getUserProfile(username));
    }

    @GetMapping("/backoffice/users/{userId}")
    public ResponseEntity<UserDetailGetDto> getUserByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserByUserId(userId));
    }

    @PostMapping("/storefront/users")
    public ResponseEntity<UserDetailGetDto> createUser(@RequestBody @Valid UserPostDto userPostDto,
                                                       UriComponentsBuilder uriComponentsBuilder)
    {
        UserDetailGetDto userDetailGetDto = userService.createNewUser(userPostDto);
        URI uri = uriComponentsBuilder.replacePath("/users/{id}").buildAndExpand(userDetailGetDto.id()).toUri();
        return ResponseEntity.ok(userDetailGetDto);
    }

    @PutMapping("/backoffice/users/profile/{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable String userId,
                                           @RequestBody UserProfileUpdateDto userProfileUpdateDto) {
        userService.updateUserProfileById(userId, userProfileUpdateDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/backoffice/users/{userId}/ban")
    public ResponseEntity<Void> banUserById(@PathVariable(name = "userId") String userId) {
        userService.UserBanManagement(userId, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/backoffice/users/{userId}/unban")
    public ResponseEntity<Void> unbanUserById(@PathVariable(name = "userId") String userId) {
        userService.UserBanManagement(userId, false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/backoffice/users/profile/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }

}
