package com.jiaozhilong.gisagent.security;

import com.jiaozhilong.gisagent.role.PermissionEntity;
import com.jiaozhilong.gisagent.role.RoleEntity;
import com.jiaozhilong.gisagent.user.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record PlatformUserPrincipal(UUID id, String username, String password, boolean enabled,
                                    Collection<? extends GrantedAuthority> authorities) implements UserDetails {
    public static PlatformUserPrincipal from(UserEntity user) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (RoleEntity role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().name()));
            for (PermissionEntity permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }
        return new PlatformUserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getStatus() == com.jiaozhilong.gisagent.user.UserStatus.ACTIVE, authorities);
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
}
