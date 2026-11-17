package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.RbacProperties;
import com.harmoni.menu.dashboard.dto.PermissionDto;
import com.harmoni.menu.dashboard.dto.RoleDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blocking REST client for the access-control endpoints exposed by the
 * authentication service.
 */
@Service
@Slf4j
public class RestClientRbacService extends RestClientService {

    private final RbacProperties rbacProperties;

    public RestClientRbacService(RbacProperties rbacProperties, TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.rbacProperties = rbacProperties;
    }

    public Mono<RestAPIResponse> getRoles() {
        return get(rbacProperties.getUrl().getRoles());
    }

    public Mono<RestAPIResponse> createRole(RoleDto roleDto) {
        return post(rbacProperties.getUrl().getRoles(), Mono.just(roleDto), RoleDto.class);
    }

    public Mono<RestAPIResponse> updateRole(RoleDto roleDto) {
        return put(String.format(rbacProperties.getUrl().getRoleById(), roleDto.getId()),
                Mono.just(roleDto), RoleDto.class);
    }

    public Mono<RestAPIResponse> deleteRole(Long id) {
        return delete(String.format(rbacProperties.getUrl().getRoleById(), id));
    }

    public Mono<RestAPIResponse> replaceRolePermissions(Long id, Set<Long> permissionIds) {
        return put(String.format(rbacProperties.getUrl().getRolePermissions(), id),
                Mono.just(permissionIds), Set.class);
    }

    public Mono<RestAPIResponse> getPermissions() {
        return get(rbacProperties.getUrl().getPermissions());
    }

    public Mono<RestAPIResponse> createPermission(PermissionDto permissionDto) {
        return post(rbacProperties.getUrl().getPermissions(), Mono.just(permissionDto), PermissionDto.class);
    }

    public Mono<RestAPIResponse> updatePermission(PermissionDto permissionDto) {
        return put(String.format(rbacProperties.getUrl().getPermissionById(), permissionDto.getId()),
                Mono.just(permissionDto), PermissionDto.class);
    }

    public Mono<RestAPIResponse> deletePermission(Long id) {
        return delete(String.format(rbacProperties.getUrl().getPermissionById(), id));
    }

    public Mono<RestAPIResponse> getUserRoles(Long userId) {
        return get(String.format(rbacProperties.getUrl().getUserRoles(), userId));
    }

    public Mono<RestAPIResponse> duplicateRole(Long id) {
        return post(String.format(rbacProperties.getUrl().getRoleDuplicate(), id),
                Mono.empty(), RoleDto.class);
    }

    public Mono<RestAPIResponse> batchAssignments(List<Long> userIds) {
        Map<String, List<Long>> payload = Map.of("userIds", userIds);
        return post(rbacProperties.getUrl().getAssignmentsBatch(), Mono.just(payload), Map.class);
    }

    public Mono<RestAPIResponse> getMyAuthorities(String token) {
        return getWithoutLogout(rbacProperties.getUrl().getMe(), token);
    }
}
