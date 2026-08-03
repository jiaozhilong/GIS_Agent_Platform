package com.jiaozhilong.gisagent.role;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "platform_roles")
public class RoleEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 40)
    private RoleCode code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(name = "built_in", nullable = false)
    private boolean builtIn;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "platform_role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<PermissionEntity> permissions = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public RoleCode getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isBuiltIn() { return builtIn; }
    public Set<PermissionEntity> getPermissions() { return permissions; }
}
