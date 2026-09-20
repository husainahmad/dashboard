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
 * Called when the user clicks Update on the customization form: validates the
 * main form and every option row, posts the updated customization and
 * broadcasts the result.
 */
@RequiredArgsConstructor
@Slf4j
public class CustomizationUpdateEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final CustomizationForm customizationForm;
    private final RestClientMenuService restClientMenuService;

    /**
     * Validates the main customization form and its option rows and, if valid,
     * posts the updated customization.
     *
     * @param event the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        CustomizationDto customization = CustomizationDto.builder().build();

        if (!customizationForm.getCustomizationBinder().writeBeanIfValid(customization)) {
            showNotification("Please fix the errors in the form.", NotificationVariant.LUMO_ERROR);
            return;
        }

        List<CustomizationOptionDto> options = customizationForm.buildOptions();
        if (options == null) {
            showNotification("Please fill all required option fields.", NotificationVariant.LUMO_ERROR);
            return;
        }

        if (options.isEmpty()) {
            showNotification("A customization must have at least one customization option.", NotificationVariant.LUMO_ERROR);
            return;
        }

        customization.setId(customizationForm.getCustomizationId());
        customization.setCustomizationOptions(options);

        restClientMenuService.updateCustomization(customization)
                .subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        broadcastMessage(BroadcastMessage.CUSTOMIZATION_UPDATED_SUCCESS, response);
        customizationForm.onUpdateSuccess();
    }

    private void onError(Throwable error) {
        log.error("Update customization failed", error);
        if (!(error instanceof BusinessBadRequestException)) {
            customizationForm.onSaveError(error);
        }
    }

    private void showNotification(String text, NotificationVariant variant) {
        UiUtil.show(text, variant, 3000);
    }
}