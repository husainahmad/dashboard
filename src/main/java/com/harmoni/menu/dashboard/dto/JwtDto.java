package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class JwtDto implements Serializable {
    private String accessToken;
    private String refreshToken;
}
