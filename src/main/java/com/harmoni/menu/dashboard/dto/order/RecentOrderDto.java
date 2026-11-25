package com.harmoni.menu.dashboard.dto.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class RecentOrderDto implements Serializable {

    private Integer id;

    private String orderNo;

    private BigDecimal grandTotal;

    private String status;

    private String tableNumber;

    private Integer storeServiceTypesId;

    private Date createdAt;

    private List<OrderLineDto> orderDetails;
}
