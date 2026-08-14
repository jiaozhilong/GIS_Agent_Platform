package com.jiaozhilong.gisagent.auth;

import com.jiaozhilong.gisagent.role.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String account, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank @Size(min = 3, max = 50) String username,
                                  @NotBlank @Size(max = 100) String displayName,
                                  @NotBlank @Email @Size(max = 160) String email,
                                  @Size(max = 30) String phone,
                                  @Size(max = 120) String department,
                                  @NotBlank @Size(min = 8, max = 72) String password) {}
    public record UserProfile(UUID id, String username, String displayName, String email, RoleCode role,
                              List<RoleCode> roleCodes, List<String> permissions) {}
    public record LoginResult(String accessToken, String tokenType, long expiresIn, UserProfile user) {}
}
