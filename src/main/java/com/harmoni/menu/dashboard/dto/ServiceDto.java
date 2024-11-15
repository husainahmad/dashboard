package com.harmoni.menu.dashboard.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ServiceDto {

    private Integer id;
    @NotEmpty
    private String name;
    private List<SubServiceDto> subServices;
    private Date createdAt;
    private Date updatedAt;

}