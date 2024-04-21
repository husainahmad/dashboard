package com.harmoni.menu.dashboard.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
public class ChainDto {

    private Integer id;
    @NotEmpty
    private String name;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

}