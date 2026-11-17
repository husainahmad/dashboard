package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * An employee row: the menu user plus the scoped role assignments resolved from
 * the authentication service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {

    private Integer id;
    private Long authUserId;
    private String username;
    private String email;
    private String employeeCode;
    private String status;
    private Integer storeId;

    @Builder.Default
    private List<AssignmentDto> assignments = new ArrayList<>();
}
