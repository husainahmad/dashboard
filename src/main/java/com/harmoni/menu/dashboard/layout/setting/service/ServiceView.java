package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Unified Service page: the service list on the left and the selected service's
 * details on the right, with create/edit in a modal dialog.
 */
public class ServiceView extends VerticalLayout {

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientMenuService restClientMenuService;

    private final ServiceListCard listCard = new ServiceListCard();
    private final ServiceDetailCard detailCard = new ServiceDetailCard();
    private final Button addButton = new Button(Messages.get(Messages.Keys.ACTION_NEW_SERVICE),
            event -> openDialog(null));

    private List<ServiceDto> services = new ArrayList<>();
    private ServiceDto selectedService;
    private Integer pendingSelectId;
    private boolean loaded;

    public ServiceView(AsyncRestClientSettingService asyncRestClientSettingService,
                       RestClientMenuService restClientMenuService) {
        this.asyncRestClientSettingService = asyncRestClientSettingService;
        this.restClientMenuService = restClientMenuService;

        addClassName("service-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        PageHeader header = PageHeader.of("page.service.title", "page.service.description");

        addButton.setIcon(VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.addClassName("service-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        HorizontalLayout body = new HorizontalLayout(listCard, detailCard);
        body.addClassName("service-body");
        body.setWidthFull();
        body.setSpacing(true);

        add(header, toolbar, body);
        setFlexGrow(1, body);

        listCard.setSelectionListener(this::selectService);
        detailCard.setEditListener(this::openDialog);
        detailCard.setDeleteListener(this::confirmDelete);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loaded = true;
            fetchServices();
        }
    }

    private void fetchServices() {
        listCard.setLoading(true);
        asyncRestClientSettingService.getAllService(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    services = result == null ? new ArrayList<>() : new ArrayList<>(result);
                    listCard.setServices(services);

                    ServiceDto target = findSelected();
                    if (target == null && !services.isEmpty()) {
                        target = services.getFirst();
                    }
                    selectService(target);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_SERVICE_LOAD_FAILED),
                            this::fetchServices);
                }));
    }

    private ServiceDto findSelected() {
        Integer id = pendingSelectId != null ? pendingSelectId
                : selectedService == null ? null : selectedService.getId();
        if (id == null) {
            return null;
        }
        ServiceDto match = services.stream()
                .filter(service -> Objects.equals(service.getId(), id))
                .findFirst()
                .orElse(null);
        if (match != null) {
            pendingSelectId = null;
        }
        return match;
    }

    private void selectService(ServiceDto service) {
        selectedService = service;
        listCard.select(service);
        detailCard.show(service);
    }

    private void openDialog(ServiceDto service) {
        new ServiceDialog(restClientMenuService, service, () -> {
            pendingSelectId = service == null ? null : service.getId();
            fetchServices();
        }).open();
    }

    private void confirmDelete(ServiceDto service) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("service.delete.confirm",
                service.getName() == null ? "" : service.getName()));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get(Messages.Keys.ACTION_DELETE));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> restClientMenuService.deleteService(service).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("service.deleted"));
                    selectedService = null;
                    fetchServices();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("service.deleteFailed")))));
        dialog.open();
    }
}
