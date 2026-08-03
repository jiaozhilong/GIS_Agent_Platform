package com.jiaozhilong.gisagent.user;

import com.jiaozhilong.gisagent.role.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}

    public record SystemUserResponse(UUID id, String username, String displayName, String email, String phone,
                                     String department, UserStatus status, List<RoleCode> roleCodes,
                                     OffsetDateTime lastLoginAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Email @Size(max = 160) String email,
            @Size(max = 30) String phone,
            @Size(max = 120) String department,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotEmpty List<RoleCode> roleCodes) {}

    public record UpdateUserRequest(@NotBlank @Size(max = 100) String displayName,
                                    @NotBlank @Email @Size(max = 160) String email,
                                    @Size(max = 30) String phone,
                                    @Size(max = 120) String department) {}
    public record UpdateUserStatusRequest(@NotNull UserStatus status) {}
    public record AssignUserRolesRequest(@NotEmpty List<RoleCode> roleCodes) {}
    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 72) String newPassword) {}
}
