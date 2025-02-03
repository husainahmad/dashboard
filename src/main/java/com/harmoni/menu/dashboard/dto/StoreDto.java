package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

@Data
public class StoreDto {

    private Integer id;
    @NotEmpty
    private String name;
    @NotEmpty
    private Integer chainId;
    @JsonProperty("chain")
    private ChainDto chainDto;
    private Integer tierMenuId;
    private Integer tierPriceId;
    private Integer tierServiceId;

    private String address;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

}