package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.OrganizationTreeDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.ActionPermissions;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified Tier page: a brand selector, a type selector, the tier list on the
 * left and the selected tier's settings on the right.
 */
public class TierView extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final boolean canEdit;

    private final ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get("tier.brand"));
    private final RadioButtonGroup<TierTypeDto> typeGroup = new RadioButtonGroup<>();
    private final TierListCard listCard = new TierListCard();
    private final TierDetailCard detailCard;

    private final Map<Integer, Integer> storeCounts = new HashMap<>();
    private final Map<Integer, List<String>> storeNamesByTier = new HashMap<>();
    private List<BrandDto> brands = new ArrayList<>();
    private List<TierRow> allRows = new ArrayList<>();

    public TierView(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                    RestClientOrganizationService restClientOrganizationService,
                    AsyncRestClientMenuService asyncRestClientMenuService,
                    RestClientMenuService restClientMenuService,
                    AuthorizationService authorizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.restClientMenuService = restClientMenuService;
        this.canEdit = ActionPermissions.canEditOrganization(authorizationService);
        this.detailCard = new TierDetailCard(asyncRestClientMenuService, restClientMenuService,
                asyncRestClientOrganizationService, restClientOrganizationService, this::loadTiers,
                canEdit);

        addClassName("tier-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        PageHeader header = new PageHeader(Messages.get("tier.title"), Messages.get("tier.description"));

        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setWidth("240px");
        brandBox.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                loadTiers();
            }
        });

        typeGroup.addClassName("org-choice-chips");
        typeGroup.setItems(TierTypeDto.PRICE, TierTypeDto.SERVICE, TierTypeDto.MENU);
        typeGroup.setValue(TierTypeDto.PRICE);
        typeGroup.setItemLabelGenerator(this::typeLabel);
        typeGroup.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                applyTypeFilter();
            }
        });

        Button addTier = new Button(Messages.get("tier.add"), event -> openTierDialog());
        addTier.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTier.setVisible(canEdit);

        HorizontalLayout filters = new HorizontalLayout(brandBox, typeGroup);
        filters.setAlignItems(FlexComponent.Alignment.CENTER);
        filters.setSpacing(true);

        HorizontalLayout toolbar = new HorizontalLayout(filters, addTier);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.addClassName("tier-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout body = new HorizontalLayout(listCard, detailCard);
        body.addClassName("tier-body");
        body.setWidthFull();
        body.setSpacing(true);

        add(header, toolbar, body);
        setFlexGrow(1, body);

        listCard.setSelectionListener(row -> detailCard.show(row, storeNamesByTier));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (brands.isEmpty()) {
            loadBrands();
        }
    }

    private void loadBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    ScopeService scopeService = ScopeService.current();
                    ScopeService.ScopeSnapshot scope = scopeService == null ? null : scopeService.snapshot();
                    List<BrandDto> allowed = scope == null ? result : scope.filterBrands(result);
                    brands = allowed == null ? new ArrayList<>() : allowed;
                    brandBox.setItems(brands);
                    brandBox.setEnabled(scope == null ? brands.size() > 1
                            : scope.canSelectBrand(brands.size()));
                    if (!brands.isEmpty()) {
                        brandBox.setValue(scope == null ? brands.getFirst() : scope.defaultBrand(brands));
                        loadTiers();
                    }
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.errorWithRetry(Messages.get("tier.loadFailed"), this::loadBrands)));
    }

    private void loadTiers() {
        BrandDto brand = brandBox.getValue();
        if (brand == null) {
            return;
        }
        asyncRestClientOrganizationService.getAllTierByBrandAsync(tiers ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    loadStoreCounts(tiers == null ? new ArrayList<>() : tiers);
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.errorWithRetry(Messages.get("tier.loadFailed"), this::loadTiers)),
                brand.getId());
    }

    private void loadStoreCounts(List<TierDto> tiers) {
        asyncRestClientOrganizationService.getOrganizationTreeAsync(tree ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    computeStoreCounts(tree);
                    allRows = new ArrayList<>();
                    for (TierDto tier : tiers) {
                        allRows.add(new TierRow(tier, storeCounts.getOrDefault(tier.getId(), 0)));
                    }
                    applyTypeFilter();
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.errorWithRetry(Messages.get("tier.loadFailed"), this::loadTiers)));
    }

    private void computeStoreCounts(OrganizationTreeDto tree) {
        storeCounts.clear();
        storeNamesByTier.clear();
        if (tree == null || tree.getBrands() == null) {
            return;
        }
        ScopeService scopeService = ScopeService.current();
        ScopeService.ScopeSnapshot scope = scopeService == null ? null : scopeService.snapshot();
        for (OrganizationTreeDto.BrandNode brand : tree.getBrands()) {
            if (scope != null && !scope.canAccessBrand(brand.getId())) {
                continue;
            }
            if (brand.getChains() == null) {
                continue;
            }
            for (OrganizationTreeDto.ChainNode chain : brand.getChains()) {
                if (scope != null && !scope.canAccessChain(chain.getId(), brand.getId())) {
                    continue;
                }
                if (chain.getStores() == null) {
                    continue;
                }
                for (OrganizationTreeDto.StoreNode store : chain.getStores()) {
                    if (scope != null && !scope.canAccessStore(store.getId(), chain.getId())) {
                        continue;
                    }
                    countTier(store.getTierPriceId(), store.getName());
                    countTier(store.getTierMenuId(), store.getName());
                    countTier(store.getTierServiceId(), store.getName());
                }
            }
        }
    }

    private void countTier(Integer tierId, String storeName) {
        if (tierId == null) {
            return;
        }
        storeCounts.merge(tierId, 1, Integer::sum);
        storeNamesByTier.computeIfAbsent(tierId, key -> new ArrayList<>()).add(storeName);
    }

    private void applyTypeFilter() {
        TierTypeDto type = typeGroup.getValue();
        List<TierRow> rows = allRows.stream()
                .filter(row -> type == null || row.getTier().getType() == type)
                .toList();
        listCard.setRows(rows);
        if (!rows.isEmpty()) {
            listCard.select(rows.getFirst());
            detailCard.show(rows.getFirst(), storeNamesByTier);
        } else {
            detailCard.show(null, storeNamesByTier);
        }
    }

    private String typeLabel(TierTypeDto type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case SERVICE -> Messages.get("tier.type.service");
            case MENU -> Messages.get("tier.type.menu");
            default -> Messages.get("tier.type.price");
        };
    }

    private void openTierDialog() {
        if (!canEdit) {
            return;
        }
        new TierDialog(restClientOrganizationService, brands, brandBox.getValue(),
                typeGroup.getValue(), this::loadTiers).open();
    }
}
