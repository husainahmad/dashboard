package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A role and the permissions granted to it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {

    private Long id;
    private String name;
    private String description;
    private String scopeType;
    private String code;
    private boolean system;
    private Integer employeeCount;

    @Builder.Default
    private Set<PermissionDto> permissions = new LinkedHashSet<>();
}
