package com.jiaozhilong.gisagent.role;

import com.jiaozhilong.gisagent.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RoleController {
    private final RoleService roleService;
    public RoleController(RoleService roleService) { this.roleService = roleService; }

    @GetMapping("/roles") @PreAuthorize("hasAuthority('role:view')")
    public ApiResponse<List<RoleDtos.RoleSummary>> roles() { return ApiResponse.ok(roleService.roles()); }

    @GetMapping("/permissions") @PreAuthorize("hasAuthority('role:view')")
    public ApiResponse<List<RoleDtos.PermissionItem>> permissions() { return ApiResponse.ok(roleService.permissions()); }
}
