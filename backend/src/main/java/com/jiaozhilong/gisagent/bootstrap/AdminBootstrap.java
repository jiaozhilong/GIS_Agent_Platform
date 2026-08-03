package com.jiaozhilong.gisagent.bootstrap;

import com.jiaozhilong.gisagent.role.RoleCode;
import com.jiaozhilong.gisagent.role.RoleRepository;
import com.jiaozhilong.gisagent.user.UserEntity;
import com.jiaozhilong.gisagent.user.UserRepository;
import com.jiaozhilong.gisagent.user.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;

    public AdminBootstrap(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                          @Value("${platform.bootstrap.admin-password}") String initialPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase("admin")) return;
        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setDisplayName("系统管理员");
        admin.setEmail("admin@gis-agent.local");
        admin.setDepartment("平台管理部");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setPasswordHash(passwordEncoder.encode(initialPassword));
        admin.setRoles(new LinkedHashSet<>(roleRepository.findByCode(RoleCode.ADMIN).stream().toList()));
        userRepository.save(admin);
    }
}
