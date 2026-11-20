package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierMenuDto;
import com.harmoni.menu.dashboard.layout.component.PaginationBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Menu panel of the Tier page: toggles the categories that belong to the
 * selected menu tier.
 */
public class TierMenuPanel extends TierPanel {

    private static final int PAGE_SIZE = 10;

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final TierDto tier;
    private final Runnable onSaved;

    private final Map<Integer, Boolean> values = new HashMap<>();
    private final PaginationBar paginationBar = new PaginationBar(this::previousPage, this::nextPage);
    private List<CategoryDto> categories = new ArrayList<>();
    private List<TierMenuDto> assignments = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 0;
    private boolean loaded;

    public TierMenuPanel(AsyncRestClientMenuService asyncRestClientMenuService,
                         AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                         RestClientOrganizationService restClientOrganizationService,
                         TierDto tier, Runnable onSaved, boolean readOnly) {
        super(readOnly);
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.tier = tier;
        this.onSaved = onSaved;
        addClassName("tier-menu-list");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loadAssignments();
        }
    }

    private void loadAssignments() {
        asyncRestClientOrganizationService.getTierMenuByBrandAsync(result ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    assignments = result == null ? new ArrayList<>() : result;
                    loadPage();
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.error(Messages.get("tier.menu.loadFailed"))),
                tier.getBrandId());
    }

    private void loadPage() {
        asyncRestClientMenuService.getAllCategoryByBrandPageAsync(result ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    categories = new ArrayList<>();
                    Object data = result == null ? null : result.get("data");
                    if (data instanceof List<?> list) {
                        for (Object item : list) {
                            categories.add(ObjectUtil.convertValueToObject(item, CategoryDto.class));
                        }
                    }
                    totalPages = result == null || result.get("page") == null
                            ? 0 : Integer.parseInt(result.get("page").toString());
                    loaded = true;
                    initValues();
                    render();
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.error(Messages.get("tier.menu.loadFailed"))),
                tier.getBrandId(), currentPage, PAGE_SIZE);
    }

    private void initValues() {
        for (CategoryDto category : categories) {
            values.putIfAbsent(category.getId(), isActive(category.getId()));
        }
    }

    private void render() {
        removeAll();

        for (CategoryDto category : categories) {
            Checkbox checkbox = new Checkbox(category.getName());
            checkbox.setValue(values.getOrDefault(category.getId(), false));
            checkbox.addClassName("tier-menu-checkbox");
            checkbox.setReadOnly(isReadOnly());
            if (!isReadOnly()) {
                checkbox.addValueChangeListener(event -> {
                    values.put(category.getId(), event.getValue());
                    markDirty();
                });
            }
            add(checkbox);
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

    private boolean isActive(Integer categoryId) {
        return assignments.stream()
                .filter(assignment -> assignment.getTierDto() != null
                        && Objects.equals(assignment.getTierDto().getId(), tier.getId()))
                .anyMatch(assignment -> assignment.getCategoryDto() != null
                        && Objects.equals(assignment.getCategoryDto().getId(), categoryId)
                        && Boolean.TRUE.equals(assignment.getActive()));
    }

    @Override
    public void save() {
        if (isReadOnly()) {
            return;
        }
        List<TierMenuDto> payload = new ArrayList<>();
        for (Map.Entry<Integer, Boolean> entry : values.entrySet()) {
            CategoryDto ref = new CategoryDto();
            ref.setId(entry.getKey());
            ref.setBrandId(tier.getBrandId());
            TierMenuDto dto = new TierMenuDto();
            dto.setTierDto(tier);
            dto.setCategoryDto(ref);
            dto.setActive(entry.getValue());
            payload.add(dto);
        }
        restClientOrganizationService.updateTierMenu(tier, payload).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("tier.menu.saved"));
                    if (onSaved != null) {
                        onSaved.run();
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("tier.menu.saveFailed"))));
    }
}
