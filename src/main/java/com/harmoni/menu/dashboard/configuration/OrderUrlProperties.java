package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderUrlProperties implements Serializable {

    private String page = "http://localhost:8080/api/v1/order/page";
}
