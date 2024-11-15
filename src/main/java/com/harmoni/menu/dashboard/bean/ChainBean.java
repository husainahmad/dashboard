package com.harmoni.menu.dashboard.bean;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChainBean {

    @NotEmpty
    private String name;

}