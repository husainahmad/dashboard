package com.harmoni.menu.dashboard.dto;

import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Master definition of a promotion, used both as the create/update payload and as
 * the response body of the {@code /api/v1/promotion} endpoints.
 * <p>
 * On update an omitted child collection leaves the stored rows untouched, while an
 * empty list clears them, so the form always submits every collection it manages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {

    /** The database id, or {@code null} when the promotion has not been saved yet. */

    private Long id;

    /** The unique short code an order references to claim the discount. */

    private String code;

    /** The operator-facing name. */

    private String name;

    /** Optional free-text description. */

    private String description;

    /** The mechanism the promotion applies. */

    private PromotionType promotionType;

    /** The lifecycle state. */

    private PromotionStatus status;

    /** Lower numbers win when several promotions could apply to one basket. */

    private Integer priority;

    /** Optional inclusive first day the promotion may be redeemed on. */

    private LocalDate startDate;

    /** Optional inclusive last day the promotion may be redeemed on. */

    private LocalDate endDate;

    /** The weekly time windows the promotion is live in. */

    private List<PromotionScheduleDto> schedules;

    /** The products, SKUs and categories the promotion applies to. */

    private List<PromotionTargetDto> targets;

    /** The constraints a basket must satisfy. */

    private List<PromotionRuleDto> rules;

    /** The replacement prices, only used by a {@code SPECIAL_PRICE} promotion. */

    private List<PromotionSpecialPriceDto> specialPrices;

}
