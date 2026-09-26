package com.harmoni.menu.dashboard.event.promotion;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.menu.promotion.PromotionForm;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Called when the user saves a promotion opened in {@code STATUS} mode: applies
 * only the chosen lifecycle state through the dedicated status endpoint, leaving
 * the configuration untouched.
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionStatusEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final PromotionForm promotionForm;
    private final RestClientPromotionService restClientPromotionService;

    /**
     * Transitions the promotion to the state currently selected in the form.
     *
     * @param event the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        PromotionStatus status = promotionForm.getStatusSelect().getValue();
        Long promotionId = promotionForm.getPromotionDto() == null
                ? null
                : promotionForm.getPromotionDto().getId();
        if (promotionId == null || status == null) {
            UiUtil.error(Messages.get("validation.promotion.statusRequired"));
            return;
        }
        restClientPromotionService.updatePromotionStatus(promotionId, status.name())
                .subscribe(response -> accept(response, status),
                        this::onError);
    }

    private void accept(RestAPIResponse response, PromotionStatus status) {
        broadcastMessage(BroadcastMessage.PROMOTION_STATUS_UPDATED_SUCCESS, response);
        UI ui = promotionForm.getUi();
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            UiUtil.success(Messages.get("notification.promotion.statusUpdated", status.getLabel()));
            promotionForm.close();
        });
    }

    private void onError(Throwable error) {
        log.error("Update promotion status failed", error);
        if (!(error instanceof BusinessBadRequestException)) {
            UiUtil.error(Messages.get("notification.promotion.statusFailed"));
        }
    }

    /**
     * The states an operator may move a promotion to from the grid, offered in the
     * order they normally happen in.
     *
     * @return the transitions available for a live promotion
     */
    public static List<PromotionStatus> commonTransitions() {
        return Arrays.asList(PromotionStatus.ACTIVE, PromotionStatus.PAUSED,
                PromotionStatus.SCHEDULED, PromotionStatus.EXPIRED, PromotionStatus.CANCELLED);
    }
}
