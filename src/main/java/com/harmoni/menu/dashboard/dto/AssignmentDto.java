package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A role assignment with its scope.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {

    private Long id;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private String scopeType;
    private Long scopeId;
}
