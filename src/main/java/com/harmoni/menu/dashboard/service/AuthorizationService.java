package com.harmoni.menu.dashboard.service;

import com.harmoni.menu.dashboard.dto.AssignmentDto;
import com.harmoni.menu.dashboard.dto.AuthoritiesDto;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Reads the signed-in user's effective roles and permissions from the session
 * and answers the access questions the UI asks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService implements Serializable {

    /**
     * Stores the authorities resolved at login.
     *
     * @param authorities the roles and permissions to keep for this session
     */
    public void store(AuthoritiesDto authorities) {
        VaadinSessionUtil.setAttribute(VaadinSessionUtil.AUTHORITIES, authorities);
    }

    /**
     * Returns the permissions granted to the signed-in user.
     *
     * @return the permission names, never {@code null}
     */
    public Set<String> getPermissions() {
        AuthoritiesDto authorities = VaadinSessionUtil.getAttribute(
                VaadinSessionUtil.AUTHORITIES, AuthoritiesDto.class);
        if (authorities == null) {
            log.warn("No authorities in session; denying access until the next login");
            return Set.of();
        }
        return authorities.getPermissions() == null ? Set.of() : authorities.getPermissions();
    }

    /**
     * Returns the scoped role assignments of the signed-in user.
     *
     * @return the assignments, never {@code null}
     */
    public List<AssignmentDto> getScopes() {
        AuthoritiesDto authorities = VaadinSessionUtil.getAttribute(
                VaadinSessionUtil.AUTHORITIES, AuthoritiesDto.class);
        if (authorities == null || authorities.getScopes() == null) {
            return List.of();
        }
        return authorities.getScopes();
    }

    /**
     * Returns the role codes granted to the signed-in user, e.g. {@code SYS}.
     *
     * @return the role codes, never {@code null}
     */
    public Set<String> getRoleCodes() {
        AuthoritiesDto authorities = VaadinSessionUtil.getAttribute(
                VaadinSessionUtil.AUTHORITIES, AuthoritiesDto.class);
        if (authorities == null) {
            return Set.of();
        }
        return authorities.getRoleCodes() == null ? Set.of() : authorities.getRoleCodes();
    }

    /**
     * Whether the signed-in user holds the given role code.
     *
     * @param roleCode the role code, e.g. {@code SYS}
     * @return {@code true} when granted
     */
    public boolean hasRoleCode(String roleCode) {
        return roleCode != null && getRoleCodes().contains(roleCode);
    }

    /**
     * Whether the signed-in user holds the given permission.
     *
     * @param permission the permission name, e.g. {@code catalog:read}
     * @return {@code true} when granted
     */
    public boolean hasPermission(String permission) {
        return permission != null && getPermissions().contains(permission);
    }

    /**
     * Whether the signed-in user holds any of the given permissions.
     *
     * @param permissions the permission names to test
     * @return {@code true} when at least one is granted
     */
    public boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        Set<String> granted = getPermissions();
        return Arrays.stream(permissions).anyMatch(granted::contains);
    }
}
