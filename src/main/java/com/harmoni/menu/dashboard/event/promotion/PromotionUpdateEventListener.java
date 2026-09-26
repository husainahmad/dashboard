package com.harmoni.menu.dashboard.event.promotion;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.menu.promotion.PromotionForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called when the user clicks Update on an existing promotion: validates the form
 * and sends the whole aggregate, so the four child grids are replaced in one
 * transaction rather than row by row.
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionUpdateEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final PromotionForm promotionForm;
    private final RestClientPromotionService restClientPromotionService;

    /**
     * Validates the form and, if valid, puts the promotion together with its
     * schedules, targets, rules and special prices.
     *
     * @param event the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        if (!promotionForm.isAggregateValid()) {
            return;
        }
        PromotionDto promotion = promotionForm.buildAggregate();
        restClientPromotionService.updatePromotion(promotion)
                .subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        broadcastMessage(BroadcastMessage.PROMOTION_UPDATED_SUCCESS, response);
        promotionForm.onSaveSuccess();
    }

    private void onError(Throwable error) {
        log.error("Update promotion failed", error);
        if (!(error instanceof BusinessBadRequestException)) {
            promotionForm.onSaveError(error);
        }
    }
}
