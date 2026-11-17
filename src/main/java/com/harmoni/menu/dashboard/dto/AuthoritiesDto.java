package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The effective roles, permissions and scoped assignments of the signed-in user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthoritiesDto {

    private Integer userId;
    private String username;

    @Builder.Default
    private Set<String> roles = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> roleCodes = new LinkedHashSet<>();

    @Builder.Default
    private Set<String> permissions = new LinkedHashSet<>();

    @Builder.Default
    private List<AssignmentDto> scopes = new ArrayList<>();
}
