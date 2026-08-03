package com.jiaozhilong.gisagent.user;

import com.jiaozhilong.gisagent.common.api.PageResponse;
import com.jiaozhilong.gisagent.common.exception.BusinessException;
import com.jiaozhilong.gisagent.role.RoleCode;
import com.jiaozhilong.gisagent.role.RoleEntity;
import com.jiaozhilong.gisagent.role.RoleRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDtos.SystemUserResponse> list(String keyword, UserStatus status, RoleCode roleCode, int page, int pageSize) {
        Specification<UserEntity> specification = Specification.where(null);
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT).trim() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), pattern), cb.like(cb.lower(root.get("displayName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern), cb.like(cb.lower(root.get("department")), pattern)));
        }
        if (status != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (roleCode != null) specification = specification.and((root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("roles", JoinType.INNER).get("code"), roleCode);
        });
        Page<UserDtos.SystemUserResponse> result = userRepository.findAll(specification,
                        PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, pageSize)), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::response);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public UserDtos.SystemUserResponse get(UUID id) { return response(require(id)); }

    @Transactional
    public UserDtos.SystemUserResponse create(UserDtos.CreateUserRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) throw conflict("登录账号已存在");
        if (userRepository.existsByEmailIgnoreCase(request.email())) throw conflict("邮箱已存在");
        UserEntity user = new UserEntity();
        user.setUsername(request.username().trim());
        user.setDisplayName(request.displayName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPhone(blankToNull(request.phone()));
        user.setDepartment(blankToNull(request.department()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(resolveRoles(request.roleCodes()));
        return response(userRepository.save(user));
    }

    @Transactional
    public UserDtos.SystemUserResponse update(UUID id, UserDtos.UpdateUserRequest request) {
        UserEntity user = require(id);
        if (!user.getEmail().equalsIgnoreCase(request.email()) && userRepository.existsByEmailIgnoreCase(request.email())) throw conflict("邮箱已存在");
        user.setDisplayName(request.displayName().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPhone(blankToNull(request.phone()));
        user.setDepartment(blankToNull(request.department()));
        return response(user);
    }

    @Transactional
    public UserDtos.SystemUserResponse updateStatus(UUID id, UserStatus status) {
        UserEntity user = require(id);
        user.setStatus(status);
        return response(user);
    }

    @Transactional
    public UserDtos.SystemUserResponse assignRoles(UUID id, List<RoleCode> roleCodes) {
        UserEntity user = require(id);
        user.setRoles(resolveRoles(roleCodes));
        return response(user);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) { require(id).setPasswordHash(passwordEncoder.encode(newPassword)); }

    UserDtos.SystemUserResponse response(UserEntity user) {
        return new UserDtos.SystemUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getPhone(), user.getDepartment(), user.getStatus(), user.getRoles().stream().map(RoleEntity::getCode).sorted().toList(),
                user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private UserEntity require(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "用户不存在"));
    }

    private Set<RoleEntity> resolveRoles(List<RoleCode> codes) {
        Set<RoleCode> unique = new LinkedHashSet<>(codes);
        List<RoleEntity> roles = roleRepository.findAllByCodeIn(unique);
        if (roles.size() != unique.size()) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "包含无效角色");
        return new LinkedHashSet<>(roles);
    }

    private String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, "CONFLICT", message); }
}
