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
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.NotificationVariant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Called when the user clicks Save on the customization form: validates the
 * main form and every option row, posts the customization with tier-based
 * prices and broadcasts the result.
 */
@RequiredArgsConstructor
@Slf4j
public class CustomizationSaveEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final CustomizationForm customizationForm;
    private final RestClientMenuService restClientMenuService;

    /**
     * Validates the main customization form and its option rows and, if valid,
     * posts the new customization.
     *
     * @param event the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        CustomizationDto customization = CustomizationDto.builder().build();

        // validate main customization form
        if (!customizationForm.getCustomizationBinder().writeBeanIfValid(customization)) {
            showNotification("Please fix the errors in the form.", NotificationVariant.LUMO_ERROR);
            return;
        }

        // validate each option row and attach tier-based prices
        List<CustomizationOptionDto> options = customizationForm.buildOptions();
        if (options == null) {
            showNotification("Please fill all required option fields.", NotificationVariant.LUMO_ERROR);
            return;
        }

        // must have at least one option
        if (options.isEmpty()) {
            showNotification("A customization must have at least one customization option.", NotificationVariant.LUMO_ERROR);
            return;
        }

        customization.setId(customizationForm.getCustomizationId());
        customization.setCustomizationOptions(options);

        // call REST API
        restClientMenuService.saveCustomization(customization)
                .subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        // notify other sessions so their grids refresh
        broadcastMessage(BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS, response);
        // handle this session's UI directly: toast + close tab (grid refresh arrives via broadcast)
        customizationForm.onSaveSuccess();
    }

    private void onError(Throwable error) {
        log.error("Save customization failed", error);
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