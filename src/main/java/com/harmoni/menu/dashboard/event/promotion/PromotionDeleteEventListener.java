package com.harmoni.menu.dashboard.event.promotion;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called when the user deletes a promotion, either from the grid row action or from
 * the footer of an open edit form: shows a confirmation dialog and, once confirmed,
 * removes the promotion together with its child collections and broadcasts the
 * result so every open grid refreshes.
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionDeleteEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final PromotionDto promotionDto;
    private final RestClientPromotionService restClientPromotionService;

    /**
     * Builds the delete button shown in the footer of an edit form, so removing a
     * promotion does not require going back to the grid first.
     *
     * @return a delete button wired to this listener
     */
    public Button asDeleteButton() {
        Button button = UiUtil.deleteButton(Messages.get(Messages.Keys.ACTION_DELETE));
        button.addClickListener(this::onComponentEvent);
        return button;
    }

    /**
     * Opens the delete confirmation dialog when the delete button is clicked.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (promotionDto == null || promotionDto.getId() == null) {
            return;
        }
        String name = promotionDto.getName() == null || promotionDto.getName().isBlank()
                ? promotionDto.getCode()
                : promotionDto.getName();
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        confirmDialog.setText(Messages.get("dialog.confirmDeletePromotion", name));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(event -> executeDelete());
        confirmDialog.open();
    }

    private void executeDelete() {
        restClientPromotionService.deletePromotion(promotionDto.getId())
                .subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        UiUtil.success(Messages.get("notification.promotion.deleted"));
        broadcastMessage(BroadcastMessage.PROMOTION_DELETE_SUCCESS, response);
    }

    private void onError(Throwable error) {
        log.error("Delete promotion failed id={}", promotionDto == null ? null : promotionDto.getId(), error);
        if (!(error instanceof BusinessBadRequestException)) {
            UiUtil.error(Messages.get("notification.promotion.deleteFailed"));
        }
    }
}
