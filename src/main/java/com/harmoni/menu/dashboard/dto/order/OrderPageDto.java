package com.harmoni.menu.dashboard.dto.order;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OrderPageDto implements Serializable {

    private List<RecentOrderDto> orders;

    private Integer nextCursor;

    private boolean hasMore;
}
