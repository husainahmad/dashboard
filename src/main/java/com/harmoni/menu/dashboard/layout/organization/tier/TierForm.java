package com.harmoni.menu.dashboard.layout.organization.tier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.tier.TierDeleteEventListener;
import com.harmoni.menu.dashboard.event.tier.TierSaveEventListener;
import com.harmoni.menu.dashboard.event.tier.TierUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
public class TierForm extends FormLayout {

    @Getter
    private RestClientOrganizationService restClientOrganizationService;
    @Getter
    private AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    private Registration broadcasterRegistration;
    @Getter
    private BeanValidationBinder<TierDto> binder = new BeanValidationBinder<>(TierDto.class);
    @Getter
    public TextField tierNameField = new TextField("Tier service name");
    @Getter
    @Setter
    public ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");
    @Getter
    MultiSelectComboBox<ServiceDto> serviceBox = new MultiSelectComboBox<>("Service");

    protected final Button saveButton = new Button("Save");
    protected final Button  deleteButton = new Button("Delete");
    protected final Button  closeButton = new Button("Cancel");
    protected final Button  updateButton = new Button("Update");

    @Getter
    private UI ui;
    @Getter
    private transient TierDto tierDto;
    @Setter
    @Getter
    private transient List<BrandDto> brandDtos;

    public TierForm(RestClientOrganizationService restClientOrganizationService, AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                    if (broadcastMessage.getType().equals(BroadcastMessage.TIER_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS)) {
                        showNotification("Tier updated..");
                        hideForm();
                    }
                    if (broadcastMessage.getType().equals(BroadcastMessage.BAD_REQUEST_FAILED)) {
                        showErrorDialog(message);
                    }
                    if (broadcastMessage.getType().equals(BroadcastMessage.PROCESS_FAILED)) {
                        showErrorDialog(message);
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        ui.access(() -> brandBox.setItems(brandDtos));
    }

    public void showNotification(String text) {
        ui.access(()->{
            Notification notification = new Notification(text, 3000,
                    Notification.Position.MIDDLE);
            notification.open();
        });
    }

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }

    public void hideForm() {
        ui.access(()-> this.setVisible(false));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    protected void setTierDtoAndBind(TierDto tierDto) {
        this.tierDto = (tierDto);
        getBinder().readBean(this.getTierDto());
    }

    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            closeButton.setVisible(true);
            updateButton.setVisible(false);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            closeButton.setVisible(true);
            updateButton.setVisible(true);
        }
    }

    protected HorizontalLayout createButtonsLayout(boolean enableUpdate, boolean enableDelete, boolean enableSave) {

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);
        saveButton.addClickListener(
                new TierSaveEventListener(this, this.getRestClientOrganizationService()));
        updateButton.addClickListener(new TierUpdateEventListener(this, this.getRestClientOrganizationService()));
        deleteButton.addClickListener(new TierDeleteEventListener(this, this.getRestClientOrganizationService()));

        closeButton.addClickListener(buttonClickEvent -> this.setVisible(false));

        if (enableSave) {
            horizontalLayout.add(saveButton);
        }
        if (enableUpdate) {
            horizontalLayout.add(updateButton);
        }
        if (enableDelete) {
            horizontalLayout.add(deleteButton);
        }

        horizontalLayout.add(closeButton);
        return horizontalLayout;
    }

}
