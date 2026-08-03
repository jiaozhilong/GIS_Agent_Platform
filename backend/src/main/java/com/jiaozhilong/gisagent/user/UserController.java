package com.jiaozhilong.gisagent.user;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import com.jiaozhilong.gisagent.common.api.PageResponse;
import com.jiaozhilong.gisagent.role.RoleCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<PageResponse<UserDtos.SystemUserResponse>> list(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) RoleCode roleCode, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.ok(userService.list(keyword, status, roleCode, page, pageSize));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<UserDtos.SystemUserResponse> get(@PathVariable UUID id) { return ApiResponse.ok(userService.get(id)); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<UserDtos.SystemUserResponse> create(@Valid @RequestBody UserDtos.CreateUserRequest request) {
        return ApiResponse.created(userService.create(request));
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<UserDtos.SystemUserResponse> update(@PathVariable UUID id, @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<UserDtos.SystemUserResponse> status(@PathVariable UUID id, @Valid @RequestBody UserDtos.UpdateUserStatusRequest request) {
        return ApiResponse.ok(userService.updateStatus(id, request.status()));
    }

    @PutMapping("/{id}/roles") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<UserDtos.SystemUserResponse> roles(@PathVariable UUID id, @Valid @RequestBody UserDtos.AssignUserRolesRequest request) {
        return ApiResponse.ok(userService.assignRoles(id, request.roleCodes()));
    }

    @PostMapping("/{id}/reset-password") @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody UserDtos.ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ApiResponse.ok(null);
    }
}
