package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A device registered to a store: a POS terminal, a table-order tablet, a
 * self-order kiosk or a quick-order device.
 *
 * <p>The {@code configuration} field is a mode-specific JSON object. The label
 * helpers render it for the grid without leaking raw JSON into the UI.</p>
 */
@Data
@Slf4j
public class DeviceDto {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Integer id;
    private Integer storeId;
    private String name;
    private String address;
    private String deviceType;
    private String deviceMode;
    private String configuration;
    private String status;

    public boolean isActive() {
        return status == null || "ACTIVE".equalsIgnoreCase(status);
    }

    /**
     * @return a translated label for the hardware type
     */
    public String typeLabel() {
        return deviceType == null ? "-" : Messages.get("device.type." + deviceType.toLowerCase());
    }

    /**
     * @return a translated label for the operating mode
     */
    public String modeLabel() {
        return deviceMode == null ? "-" : Messages.get("device.mode." + deviceMode.toLowerCase());
    }

    /**
     * Renders the mode-specific configuration as short human text, e.g.
     * {@code Cash / Card / QRIS}, {@code Table 01}, {@code Dine-in / Takeaway}
     * or {@code Counter}.
     *
     * @return the configuration caption, never {@code null}
     */
    public String configurationLabel() {
        if (configuration == null || configuration.isBlank()) {
            return "-";
        }
        try {
            Config config = MAPPER.readValue(configuration, Config.class);
            if (config == null) {
                return "-";
            }
            if (config.getPayments() != null && !config.getPayments().isEmpty()) {
                return config.getPayments().stream()
                        .map(DeviceDto::paymentLabel)
                        .collect(Collectors.joining(" / "));
            }
            if (config.getOrderTypes() != null && !config.getOrderTypes().isEmpty()) {
                return config.getOrderTypes().stream()
                        .map(DeviceDto::orderTypeLabel)
                        .collect(Collectors.joining(" / "));
            }
            List<Integer> tableIds = config.tableIds();
            if (!tableIds.isEmpty()) {
                return tableIds.stream()
                        .map(id -> Messages.get("device.config.table", id))
                        .collect(Collectors.joining(", "));
            }
            if (config.getLabel() != null && !config.getLabel().isBlank()) {
                return config.getLabel();
            }
        } catch (Exception exception) {
            log.debug("Could not read device configuration: {}", configuration, exception);
        }
        return "-";
    }

    private static String paymentLabel(String code) {
        return switch (code == null ? "" : code.toUpperCase()) {
            case "CASH" -> Messages.get("device.payment.cash");
            case "CARD" -> Messages.get("device.payment.card");
            case "QR", "QRIS" -> Messages.get("device.payment.qris");
            default -> code;
        };
    }

    private static String orderTypeLabel(String code) {
        return switch (code == null ? "" : code.toUpperCase()) {
            case "DINE_IN" -> Messages.get("device.orderType.dineIn");
            case "TAKEAWAY" -> Messages.get("device.orderType.takeaway");
            case "DELIVERY" -> Messages.get("device.orderType.delivery");
            default -> code;
        };
    }

    /**
     * Parsed shape of {@link #configuration}; unknown fields are ignored.
     */
    @Data
    public static class Config {
        private Set<String> payments = Set.of();
        private Set<String> orderTypes = Set.of();
        private List<Integer> tableIds = new ArrayList<>();
        private String label;

        /**
         * Tables configured for a table-order device.
         *
         * @return the selected table ids, never {@code null}
         */
        public List<Integer> tableIds() {
            return tableIds == null ? List.of() : tableIds;
        }
    }

    /**
     * Parses the JSON configuration of this device.
     *
     * @return the parsed configuration, never {@code null}
     */
    public Config parseConfiguration() {
        if (configuration == null || configuration.isBlank()) {
            return new Config();
        }
        try {
            Config parsed = MAPPER.readValue(configuration, Config.class);
            return parsed == null ? new Config() : parsed;
        } catch (Exception exception) {
            log.debug("Could not parse device configuration: {}", configuration, exception);
            return new Config();
        }
    }

    /**
     * @return the payments configured for a cashier device
     */
    public List<String> paymentCodes() {
        return new ArrayList<>(parseConfiguration().getPayments());
    }

    /**
     * @return the order types configured for a kiosk device
     */
    public List<String> orderTypeCodes() {
        return new ArrayList<>(parseConfiguration().getOrderTypes());
    }
}
