package com.harmoni.menu.dashboard.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

@Data
public class ServiceDto {

    private Integer id;
    @NotEmpty
    private String name;
    private Date createdAt;
    private Date updatedAt;

}