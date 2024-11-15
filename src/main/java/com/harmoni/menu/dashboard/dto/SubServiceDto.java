package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

@Data
public class SubServiceDto {

    private Integer id;
    @NotEmpty
    private String name;
    @NotEmpty
    private Integer serviceId;
    @JsonProperty("service")
    private ServiceDto serviceDto;
    private Date createdAt;
    private Date updatedAt;

}