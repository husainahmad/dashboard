package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A permission granted to a role, named {@code module:action}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {

    private Long id;
    private String name;
    private String description;
    private String module;
    private String action;
    private String category;
    private String label;
}
