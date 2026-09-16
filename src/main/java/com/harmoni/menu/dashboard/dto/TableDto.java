package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class TableDto {

    private Integer id;
    @NotEmpty
    private String name;
    @NotNull
    @Min(1)
    @Max(999)
    private Integer capacity;
    private Integer storeId;
    @JsonProperty("store")
    private StoreDto storeDto;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

}
