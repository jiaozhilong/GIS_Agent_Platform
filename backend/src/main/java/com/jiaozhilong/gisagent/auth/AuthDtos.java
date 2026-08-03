package com.jiaozhilong.gisagent.auth;

import com.jiaozhilong.gisagent.role.RoleCode;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String account, @NotBlank String password) {}
    public record UserProfile(UUID id, String username, String displayName, String email, RoleCode role,
                              List<RoleCode> roleCodes, List<String> permissions) {}
    public record LoginResult(String accessToken, String tokenType, long expiresIn, UserProfile user) {}
}
