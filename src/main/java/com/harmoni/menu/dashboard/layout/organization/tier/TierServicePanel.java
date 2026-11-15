package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.SubServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierServiceDto;
import com.harmoni.menu.dashboard.dto.TierSubServiceDto;
import com.harmoni.menu.dashboard.layout.component.PaginationBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service panel of the Tier page: toggles the sub-services that belong to the
 * selected service tier.
 */
public class TierServicePanel extends TierPanel {

    private static final int PAGE_SIZE = 5;

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final TierDto tier;
    private final Runnable onSaved;

    private final Map<Integer, Boolean> values = new HashMap<>();
    private final PaginationBar paginationBar = new PaginationBar(this::previousPage, this::nextPage);
    private List<ServiceDto> services = new ArrayList<>();
    private List<TierServiceDto> assignments = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 0;
    private boolean loaded;

    public TierServicePanel(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                            RestClientOrganizationService restClientOrganizationService,
                            TierDto tier, Runnable onSaved) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.tier = tier;
        this.onSaved = onSaved;
        addClassName("tier-service-list");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loadAssignments();
        }
    }

    private void loadAssignments() {
        asyncRestClientOrganizationService.getTierServiceByBrandAsync(result ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    assignments = result == null ? new ArrayList<>() : result;
                    loadPage();
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.error(Messages.get("tier.service.loadFailed"))),
                tier.getBrandId());
    }

    private void loadPage() {
        asyncRestClientOrganizationService.getAllServicesPageAsync(result ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    services = new ArrayList<>();
                    Object data = result == null ? null : result.get("data");
                    if (data instanceof List<?> list) {
                        for (Object item : list) {
                            services.add(ObjectUtil.convertValueToObject(item, ServiceDto.class));
                        }
                    }
                    totalPages = result == null || result.get("page") == null
                            ? 0 : Integer.parseInt(result.get("page").toString());
                    loaded = true;
                    initValues();
                    render();
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.error(Messages.get("tier.service.loadFailed"))),
                currentPage, PAGE_SIZE);
    }

    private void initValues() {
        for (ServiceDto service : services) {
            if (service.getSubServices() == null) {
                continue;
            }
            for (SubServiceDto subService : service.getSubServices()) {
                values.putIfAbsent(subService.getId(), isActive(subService.getId()));
            }
        }
    }

    private void render() {
        removeAll();

        for (ServiceDto service : services) {
            H4 header = new H4(service.getName());
            header.addClassName("tier-service-group");
            add(header);
            if (service.getSubServices() == null) {
                continue;
            }
            for (SubServiceDto subService : service.getSubServices()) {
                Checkbox checkbox = new Checkbox(subService.getName());
                checkbox.setValue(values.getOrDefault(subService.getId(), false));
                checkbox.addClassName("tier-service-checkbox");
                checkbox.addValueChangeListener(event -> {
                    values.put(subService.getId(), event.getValue());
                    markDirty();
                });
                add(checkbox);
            }
        }

        paginationBar.update(currentPage, Math.max(totalPages, 1));
        add(paginationBar);
    }

    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadPage();
        }
    }

    private void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPage();
        }
    }

    private boolean isActive(Integer subServiceId) {
        return assignments.stream()
                .filter(assignment -> assignment.getTierDto() != null
                        && Objects.equals(assignment.getTierDto().getId(), tier.getId()))
                .anyMatch(assignment -> assignment.getSubServiceDto() != null
                        && Objects.equals(assignment.getSubServiceDto().getId(), subServiceId)
                        && assignment.isActive());
    }

    @Override
    public void save() {
        List<TierSubServiceDto> payload = new ArrayList<>();
        for (Map.Entry<Integer, Boolean> entry : values.entrySet()) {
            SubServiceDto ref = new SubServiceDto();
            ref.setId(entry.getKey());
            TierSubServiceDto dto = new TierSubServiceDto();
            dto.setTierDto(tier);
            dto.setSubServiceDto(ref);
            dto.setActive(entry.getValue());
            payload.add(dto);
        }
        restClientOrganizationService.updateTierService(tier, payload).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("tier.service.saved"));
                    if (onSaved != null) {
                        onSaved.run();
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("tier.service.saveFailed"))));
    }
}
