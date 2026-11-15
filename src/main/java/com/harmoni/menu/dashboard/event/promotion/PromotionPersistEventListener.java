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
import reactor.core.publisher.Mono;

/**
 * Called when the user clicks Save (new promotion) or Update (existing one):
 * validates the scalar fields and the child grids, then sends the whole
 * aggregate so the four child grids are replaced in one transaction rather than
 * row by row.
 *
 * <p>Save and update differ only in the endpoint and the broadcast type, so the
 * mode is fixed at construction time.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionPersistEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final PromotionForm promotionForm;
    private final RestClientPromotionService restClientPromotionService;
    private final boolean update;

    /**
     * Validates the form and, if valid, posts the promotion together with its
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
        Mono<RestAPIResponse> call = update
                ? restClientPromotionService.updatePromotion(promotion)
                : restClientPromotionService.createPromotion(promotion);
        call.subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        broadcastMessage(update
                ? BroadcastMessage.PROMOTION_UPDATED_SUCCESS
                : BroadcastMessage.PROMOTION_INSERT_SUCCESS, response);
        promotionForm.onSaveSuccess();
    }

    private void onError(Throwable error) {
        log.error("{} promotion failed", update ? "Update" : "Save", error);
        if (!(error instanceof BusinessBadRequestException)) {
            promotionForm.onSaveError(error);
        }
    }
}
