package com.harmoni.menu.dashboard.event.customization;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.menu.customization.CustomizationForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.binder.Binder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class CustomizationSaveEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final CustomizationForm customizationForm;
    private final RestClientMenuService restClientMenuService;

    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        CustomizationDto customization = CustomizationDto.builder().build();

        // validate main customization form
        if (!customizationForm.getCustomizationBinder().writeBeanIfValid(customization)) {
            showNotification("Please fix the errors in the form.", NotificationVariant.LUMO_ERROR);
            return;
        }

        // must have at least one option
        if (customizationForm.getOptionList().isEmpty()) {
            showNotification("A customization must have at least one customization option.", NotificationVariant.LUMO_ERROR);
            return;
        }

        // validate each option row
        List<CustomizationOptionDto> options = new ArrayList<>();
        for (Binder<CustomizationOptionDto> rowBinder : customizationForm.getRowBinders().values()) {
            if (!rowBinder.writeBeanIfValid(rowBinder.getBean())) {
                showNotification("Please fill all required option fields.", NotificationVariant.LUMO_ERROR);
                return;
            }
            options.add(rowBinder.getBean());
        }

        customization.setCustomizationOptions(options);

        // call REST API
        restClientMenuService.saveCustomization(customization)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse response) {
        customizationForm.getUi().access(() -> broadcastMessage(BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS, response));
    }

    private void showNotification(String text, NotificationVariant variant) {
        Notification notification = new Notification(text, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(variant);
        notification.open();
    }
}
