package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

@Route(value = "tier", layout = MainLayout.class)
@PageTitle("Tier | POSHarmoni")
public class TierListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<TierDto> tierDtoGrid = new Grid<>(TierDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final TextField filterText = new TextField();
    private UI ui;
    private final RestClientOrganizationService restClientOrganizationService;
    private TierForm tierForm;
    public TierListView(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                        @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        closeEditor();

        fetchTier();
    }

    private void closeEditor() {
        tierForm.setVisible(false);
        removeClassName("editing");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            if (message.equals(BroadcastMessage.TIER_INSERT_SUCCESS)) {
                fetchTier();
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.BAD_REQUEST.value())))) {
                showErrorDialog(message);
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())))) {
                showErrorDialog(message);
            }
        });
    }

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        tierDtoGrid.setSizeFull();
        tierDtoGrid.removeAllColumns();
        tierDtoGrid.addColumn(TierDto::getName).setHeader("Name");
        tierDtoGrid.addColumn("brandDto.name").setHeader("Brand Name");

        tierDtoGrid.getColumns().forEach(tierDtoColumn -> tierDtoColumn.setAutoWidth(true));
        tierDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
                editTier(valueChangeEvent.getValue(), FormAction.EDIT));
    }

    private void editTier(TierDto tierDto, FormAction formAction) {
        if (tierDto == null) {
            closeEditor();
        } else {
            tierForm.setTierDto(tierDto);
            tierForm.restructureButton(formAction);
            tierForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void configureForm() {
        tierForm = new TierForm(this.asyncRestClientOrganizationService,
                                this.restClientOrganizationService);
        tierForm.setWidth("25em");
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(tierDtoGrid, tierForm);
        content.setFlexGrow(2, tierDtoGrid);
        content.setFlexGrow(1, tierForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addChainButton = new Button("Add Tier");
        addChainButton.addClickListener(buttonClickEvent -> {
            addTier();
        });
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addTier() {
        tierDtoGrid.asSingleSelect().clear();
        editTier(new TierDto(), FormAction.CREATE);
    }

    private void fetchTier() {
        asyncRestClientOrganizationService.getAllTierAsync(result -> {
            ui.access(()-> {
                tierDtoGrid.setItems(result);
            });
        });
    }
}
