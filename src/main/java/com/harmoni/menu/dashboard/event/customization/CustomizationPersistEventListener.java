package com.harmoni.menu.dashboard.event.customization;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.menu.customization.CustomizationForm;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.NotificationVariant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Called when the user clicks Save (new customization) or Update (existing one)
 * on the customization form: validates the main form and every option row, posts
 * the customization with tier-based prices and broadcasts the result.
 *
 * <p>Save and update differ only in the endpoint, the broadcast type and the
 * form's success hook, so the mode is fixed at construction time.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class CustomizationPersistEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final CustomizationForm customizationForm;
    private final RestClientMenuService restClientMenuService;
    private final boolean update;

    /**
     * Validates the main customization form and its option rows and, if valid,
     * posts the customization.
     *
     * @param event the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        if (!customizationForm.validate()) {
            showNotification(Messages.get(Messages.Keys.VALIDATION_CUSTOMIZATION_FORM_ERRORS), NotificationVariant.LUMO_ERROR);
            return;
        }

        if (customizationForm.hasBlankOptionName()) {
            showNotification(Messages.get(Messages.Keys.VALIDATION_CUSTOMIZATION_FILL_OPTION_FIELDS), NotificationVariant.LUMO_ERROR);
            return;
        }

        List<CustomizationOptionDto> options = customizationForm.buildOptions();
        if (options.isEmpty()) {
            showNotification(Messages.get(Messages.Keys.VALIDATION_CUSTOMIZATION_MIN_OPTION), NotificationVariant.LUMO_ERROR);
            return;
        }

        CustomizationDto customization = customizationForm.getValue();
        customization.setId(customizationForm.getCustomizationId());
        customization.setCustomizationOptions(options);

        Mono<RestAPIResponse> call = update
                ? restClientMenuService.updateCustomization(customization)
                : restClientMenuService.saveCustomization(customization);
        call.subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        broadcastMessage(update
                ? BroadcastMessage.CUSTOMIZATION_UPDATED_SUCCESS
                : BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS, response);
        if (update) {
            customizationForm.onUpdateSuccess();
        } else {
            customizationForm.onSaveSuccess();
        }
    }

    private void onError(Throwable error) {
        log.error("{} customization failed", update ? "Update" : "Save", error);
        // BusinessBadRequestException already broadcasts BAD_REQUEST_FAILED which
        // MainLayout surfaces as an error dialog; avoid double feedback.
        if (!(error instanceof BusinessBadRequestException)) {
            customizationForm.onSaveError(error);
        }
    }

    private void showNotification(String text, NotificationVariant variant) {
        UiUtil.show(text, variant, 3000);
    }
}
