package com.jiaozhilong.gisagent.role;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "platform_permissions")
public class PermissionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 80)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 60)
    private String module;
    @Column(nullable = false, length = 500)
    private String description;

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getModule() { return module; }
    public String getDescription() { return description; }
}
