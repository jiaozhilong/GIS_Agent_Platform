package com.jiaozhilong.gisagent.role;

import com.jiaozhilong.gisagent.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDtos.RoleSummary> roles() {
        return roleRepository.findAllByOrderByCodeAsc().stream().map(role -> new RoleDtos.RoleSummary(
                role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isBuiltIn(),
                userRepository.countDistinctByRoles_Id(role.getId()), role.getPermissions().stream().map(PermissionEntity::getCode).sorted().toList())).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleDtos.PermissionItem> permissions() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc().stream()
                .map(item -> new RoleDtos.PermissionItem(item.getCode(), item.getName(), item.getModule(), item.getDescription())).toList();
    }
}
