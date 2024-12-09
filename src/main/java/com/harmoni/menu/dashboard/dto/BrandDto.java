package com.harmoni.menu.dashboard.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
public class BrandDto {

    private Integer id;
    @NotEmpty
    private String name;
    private Date createdAt;
    private Date updatedAt;

}