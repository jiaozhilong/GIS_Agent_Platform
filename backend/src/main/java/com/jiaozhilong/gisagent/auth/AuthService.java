package com.jiaozhilong.gisagent.auth;

import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.role.PermissionEntity;
import com.jiaozhilong.gisagent.role.RoleCode;
import com.jiaozhilong.gisagent.role.RoleEntity;
import com.jiaozhilong.gisagent.role.RoleRepository;
import com.jiaozhilong.gisagent.security.JwtService;
import com.jiaozhilong.gisagent.security.PlatformUserPrincipal;
import com.jiaozhilong.gisagent.user.UserEntity;
import com.jiaozhilong.gisagent.user.UserRepository;
import com.jiaozhilong.gisagent.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.LoginResult login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(request.account(), request.account())
                .orElseThrow(() -> unauthorized("账号或密码错误"));
        if (user.getStatus() != UserStatus.ACTIVE) throw unauthorized("账号已停用或锁定");
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw unauthorized("账号或密码错误");
        user.setLastLoginAt(OffsetDateTime.now());
        PlatformUserPrincipal principal = PlatformUserPrincipal.from(user);
        return new AuthDtos.LoginResult(jwtService.create(principal), "Bearer", jwtService.expirationSeconds(), profile(user));
    }

    @Transactional
    public AuthDtos.LoginResult register(AuthDtos.RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONFLICT", "用户名已存在");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONFLICT", "邮箱已存在");
        }

        RoleEntity userRole = roleRepository.findByCode(RoleCode.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role is missing"));
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setEmail(email);
        user.setPhone(blankToNull(request.phone()));
        user.setDepartment(blankToNull(request.department()));
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLastLoginAt(OffsetDateTime.now());
        user.setRoles(new LinkedHashSet<>(List.of(userRole)));
        UserEntity saved = userRepository.save(user);
        PlatformUserPrincipal principal = PlatformUserPrincipal.from(saved);
        return new AuthDtos.LoginResult(jwtService.create(principal), "Bearer", jwtService.expirationSeconds(), profile(saved));
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserProfile me(String username) {
        return profile(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                .orElseThrow(() -> unauthorized("用户不存在")));
    }

    private AuthDtos.UserProfile profile(UserEntity user) {
        List<RoleCode> roles = user.getRoles().stream().map(RoleEntity::getCode).sorted().toList();
        List<String> permissions = user.getRoles().stream().flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getCode).distinct().sorted().toList();
        RoleCode primary = roles.contains(RoleCode.ADMIN) ? RoleCode.ADMIN
                : roles.contains(RoleCode.CONSULTANT) ? RoleCode.CONSULTANT
                : roles.contains(RoleCode.REVIEWER) ? RoleCode.REVIEWER
                : RoleCode.USER;
        return new AuthDtos.UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), primary, roles, permissions);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private BusinessException unauthorized(String message) { return new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message); }
}
