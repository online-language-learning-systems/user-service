package com.hub.user_service.service;

import com.hub.common_library.exception.AccessDeniedException;
import com.hub.common_library.exception.DuplicatedException;
import com.hub.common_library.exception.ForbiddenException;
import com.hub.common_library.exception.NotFoundException;
import com.hub.user_service.config.KeycloakPropsConfig;
import com.hub.user_service.model.dto.*;
import com.hub.user_service.utils.Constants;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserService {

    private static final String ERROR_FORMAT = "%s: Client %s don't have access right for this resource";

    private final Keycloak keycloak;
    private final KeycloakPropsConfig keycloakPropsConfig;

    public UserService(Keycloak keycloak, KeycloakPropsConfig keycloakPropsConfig) {
        this.keycloak = keycloak;
        this.keycloakPropsConfig = keycloakPropsConfig;
    }

    public UserListGetDto getAllUserByRole(String role) {
        RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());
        RoleResource roleResource = realmResource.roles().get(role);
        RoleRepresentation roleRepresentation = roleResource.toRepresentation();

        Set<UserRepresentation> users = roleResource.getRoleUserMembers(0, 100);

        List<UserInfoGetDto> userInfoGetDtos = new ArrayList<>();
        users.forEach(
                user -> {
                    UserInfoGetDto userInfoGetDto = new UserInfoGetDto(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName()
                    );
                    userInfoGetDtos.add(userInfoGetDto);
                }
        );

        return new UserListGetDto(userInfoGetDtos);
    }

    public UserDetailGetDto getUserProfile(String username) {
        // Get realm
        RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());

        /*
        try {
            List<UserRepresentation> userRepresentations = realmResource.users().searchByUsername(username, true);
            if (userRepresentations.size() != 1)
                return null;

            return UserVm.fromUserRepresentation(userRepresentations.get(0));
        } catch (ForbiddenException exception) {
            throw new AccessDeniedException(
                String.format(ERROR_FORMAT, exception.getMessage(), keycloakPropsConfig.getResource())
            );
        }
         */

        try {
            return UserDetailGetDto.fromUserRepresentation(realmResource.users().get(username).toRepresentation());
        } catch (ForbiddenException exception) {
            throw new AccessDeniedException(
                    String.format(ERROR_FORMAT, exception.getMessage(), keycloakPropsConfig.getResource())
            );
        }
    }

    public UserDetailGetDto getUserByUserId(String userId) {
        try {
            // Get realm
            RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());

            // Get User Representation
            UserRepresentation userRepresentation = realmResource.users().get(userId).toRepresentation();

            return UserDetailGetDto.fromUserRepresentation(userRepresentation);
        } catch (ForbiddenException exception) {
            throw new AccessDeniedException(
                String.format(ERROR_FORMAT, exception.getMessage(), keycloakPropsConfig.getResource())
            );
        }
    }

    public UserDetailGetDto createNewUser(UserPostDto userPostDto) {
        
        if (!userPostDto.password().equals(userPostDto.passwordConfirm())) {
            throw new DuplicatedException(Constants.ErrorCode.PASSWORD_MISMATCH_ERROR, userPostDto.passwordConfirm());
        }

        // Get realm
        RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());

        if (!checkUsernameExists(realmResource, userPostDto)) {
            throw new DuplicatedException(Constants.ErrorCode.USERNAME_ALREADY_EXITED, userPostDto.username());
        }

        if (!checkEmailExists(realmResource, userPostDto)) {
            throw new DuplicatedException(Constants.ErrorCode.USER_WITH_EMAIL_ALREADY_EXITED, userPostDto.email());
        }

        // Define user
        CredentialRepresentation password = createPasswordCredentials(userPostDto.password());
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userPostDto.username());
        userRepresentation.setCredentials(Collections.singletonList(password));
        userRepresentation.setEmail(userPostDto.email());
        userRepresentation.setFirstName(userPostDto.firstName());
        userRepresentation.setLastName(userPostDto.lastName());
        userRepresentation.setEnabled(true);

        /*
            Realm Role: It is a global role, belonging to that specific realm.
                        You can access it from any client and map to any user. Ex Role: 'Global Admin, Admin'
            Client Role: It is a role which belongs only to that specific client.
                        You cannot access that role from a different client.
                        You can only map it to the Users from that client. Ex Roles: 'Employee, Customer'
            Composite Role: It is a role that has one or more roles (realm or client ones) associated to it.
         */

        Response response = realmResource.users().create(userRepresentation);


        // Add Logs
        /*
        System.out.printf("Response status: %s%n", CreatedResponseUtil.getCreatedId(response));

        if (response.hasEntity()) {
            String entity = response.readEntity(String.class); // đọc body
            System.out.printf("Response body: %s%n", entity);
        }
        */

        // Method for Assigning Role to User
        // assignRole(realmResource, response, userPostDto.role());

        // user role
        assignRole(realmResource, response, userPostDto.role().toLowerCase());

        return UserDetailGetDto.fromUserRepresentation(userRepresentation);
    }

    public void updateUserProfileById(String id, UserProfileUpdateDto userProfileUpdateDto) {
        RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());
        UserRepresentation userRepresentation = realmResource.users().get(id).toRepresentation();

        if (userRepresentation == null)
            throw new NotFoundException(Constants.ErrorCode.USER_NOT_FOUND, id);

        userRepresentation.setFirstName(userProfileUpdateDto.firstName());
        userRepresentation.setLastName(userProfileUpdateDto.lastName());
        userRepresentation.setEmail(userProfileUpdateDto.email());

        // Update the user information
        UserResource userResource = realmResource.users().get(id);
        userResource.update(userRepresentation);
    }

    public void verifyEmail(String id) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEmailVerified(true);

        // Update the user information
        RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());
        UserResource userResource = realmResource.users().get(id);
        userResource.update(userRepresentation);
    }

    public void UserBanManagement(String userId, boolean isBan) {
        RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());
        UserResource userResource = realmResource.users().get(userId);

        if (isBan) {
            banUser(userResource);
        } else {
            unbanUser(userResource);
        }
    }

    private void banUser(UserResource userResource) {

        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEnabled(false);

        // Same to Repository.save()
        userResource.update(userRepresentation);
    }

    private void unbanUser(UserResource userResource) {

        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEnabled(true);

        // Same to Repository.save()
        userResource.update(userRepresentation);
    }

    public void deleteUserById(String id) {
        RealmResource realmResource = keycloak.realm(keycloakPropsConfig.getRealm());
        UserRepresentation userRepresentation = realmResource.users().get(id).toRepresentation();

        if (userRepresentation == null)
            throw new NotFoundException(Constants.ErrorCode.USER_NOT_FOUND, id);

        userRepresentation.setEnabled(false);
        UserResource userResource = realmResource.users().get(id);
        userResource.update(userRepresentation);
    }

    public static CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(password);
        return credentialRepresentation;
    }

    private void assignRole(RealmResource realmResource, Response response, String role) {
        // Get UserResource to operate
        String userId = CreatedResponseUtil.getCreatedId(response);
        UserResource userResource = realmResource.users().get(userId);

        realmResource.roles().list().forEach(r -> System.out.println(r.getName()));

        // Assign Role to User
        RoleRepresentation roleRepresentation = realmResource.roles().get(role).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(roleRepresentation));

    }

    private boolean checkUsernameExists(RealmResource realmResource, UserPostDto userPostDto) {
        List<UserRepresentation> userRepresentations
                = realmResource.users().search(userPostDto.username(), true);
        return userRepresentations.isEmpty();
    }

    private boolean checkEmailExists(RealmResource realmResource, UserPostDto userPostDto) {
        List<UserRepresentation> userRepresentations
                = realmResource.users().searchByEmail(userPostDto.email(), true);
        return userRepresentations.isEmpty();
    }

}
