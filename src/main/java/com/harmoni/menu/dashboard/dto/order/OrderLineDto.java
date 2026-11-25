package com.harmoni.menu.dashboard.dto.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderLineDto implements Serializable {

    private String productName;

    private BigDecimal amount;
}
