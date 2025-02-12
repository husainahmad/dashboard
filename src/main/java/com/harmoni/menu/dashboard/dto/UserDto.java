package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class UserDto {
    private Integer id;
    private Integer authId;
    private String username;
    private String email;
    private String password;
    private Integer storeId;
    @JsonProperty("store")
    private StoreDto storeDto;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
}
