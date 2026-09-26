package com.harmoni.menu.dashboard.event.promotion;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.menu.promotion.PromotionForm;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called when the user clicks Save on a new promotion: validates the scalar fields
 * and the child grids, posts the whole aggregate and broadcasts the result.
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionSaveEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final PromotionForm promotionForm;
    private final RestClientPromotionService restClientPromotionService;

    /**
     * Validates the form and, if valid, posts the new promotion.
     *
     * @param event the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        if (!promotionForm.isAggregateValid()) {
            return;
        }
        PromotionDto promotion = promotionForm.buildAggregate();
        restClientPromotionService.createPromotion(promotion)
                .subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        broadcastMessage(BroadcastMessage.PROMOTION_INSERT_SUCCESS, response);
        promotionForm.onSaveSuccess();
    }

    private void onError(Throwable error) {
        log.error("Save promotion failed", error);
        if (!(error instanceof BusinessBadRequestException)) {
            promotionForm.onSaveError(error);
        }
    }
}
