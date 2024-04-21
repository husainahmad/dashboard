package com.harmoni.menu.dashboard.bean;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Builder
public class ChainBean {

    @NotEmpty
    private String name;

}