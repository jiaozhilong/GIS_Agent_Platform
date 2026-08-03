package com.jiaozhilong.gisagent.role;

import java.util.List;
import java.util.UUID;

public final class RoleDtos {
    private RoleDtos() {}
    public record RoleSummary(UUID id, RoleCode code, String name, String description, boolean builtIn,
                              long userCount, List<String> permissionCodes) {}
    public record PermissionItem(String code, String name, String module, String description) {}
}
