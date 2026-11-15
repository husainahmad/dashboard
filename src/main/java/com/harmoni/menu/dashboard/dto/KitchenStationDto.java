package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.util.List;

/**
 * A kitchen station and, when returned by the detail endpoint, its printers.
 */
@Data
public class KitchenStationDto {

    private Integer id;
    private Integer storeId;
    private String name;
    private String code;
    private String description;
    private String status;
    private List<KitchenPrinterDto> printers;

    public int printerCount() {
        return printers == null ? 0 : printers.size();
    }

    public String primaryPrinterName() {
        if (printers == null) {
            return null;
        }
        return printers.stream()
                .filter(printer -> Boolean.TRUE.equals(printer.getIsPrimary()))
                .map(KitchenPrinterDto::getName)
                .findFirst()
                .orElse(printers.isEmpty() ? null : printers.get(0).getName());
    }
}
