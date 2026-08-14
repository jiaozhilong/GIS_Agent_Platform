package com.jiaozhilong.gisagent.auth;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResult> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthDtos.LoginResult> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserProfile> me(Authentication authentication) {
        return ApiResponse.ok(authService.me(authentication.getName()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() { return ApiResponse.ok(null); }
}
