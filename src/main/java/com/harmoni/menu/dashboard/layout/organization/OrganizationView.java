package com.harmoni.menu.dashboard.layout.organization;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.OrganizationTreeDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.layout.organization.dialog.BrandDialog;
import com.harmoni.menu.dashboard.layout.organization.dialog.ChainDialog;
import com.harmoni.menu.dashboard.layout.organization.dialog.StoreDialog;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Unified Brand / Chain / Store page. A breadcrumb and a tree on the left drive
 * a detail card on the right, and the three create actions open modal dialogs.
 */
public class OrganizationView extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    private final OrganizationBreadcrumb breadcrumb = new OrganizationBreadcrumb();
    private final OrganizationTree tree = new OrganizationTree();
    private final OrganizationDetail detail;

    private final List<OrganizationNode> roots = new ArrayList<>();
    private OrganizationTreeDto treeDto;
    private OrganizationNode selectedNode;
    private boolean syncing;

    public OrganizationView(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                            RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.detail = new OrganizationDetail(restClientOrganizationService, this::loadTree,
                this::openEditDialog);

        addClassName("organization-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        OrganizationHeader header = new OrganizationHeader();

        Button addBrand = actionButton(Messages.get("organization.action.addBrand"), this::openBrandDialog);
        Button addChain = actionButton(Messages.get("organization.action.addChain"), this::openChainDialog);
        Button addStore = actionButton(Messages.get("organization.action.addStore"), this::openStoreDialog);
        addStore.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(addBrand, addChain, addStore);
        actions.addClassName("org-actions");
        actions.setSpacing(true);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout topRow = new HorizontalLayout(breadcrumb, actions);
        topRow.addClassName(Css.TOOLBAR);
        topRow.addClassName("org-top-row");
        topRow.setWidthFull();
        topRow.setAlignItems(FlexComponent.Alignment.END);
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout body = new HorizontalLayout(tree, detail);
        body.addClassName("org-body");
        body.setWidthFull();
        body.setSpacing(true);

        add(header, topRow, body);
        setFlexGrow(1, body);

        tree.setSelectionListener(this::onTreeSelected);
        breadcrumb.setSelectionListener(this::onBreadcrumbSelected);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (treeDto == null) {
            loadTree();
        }
    }

    private void loadTree() {
        asyncRestClientOrganizationService.getOrganizationTreeAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    treeDto = result;
                    buildNodes();
                    tree.setRoots(roots);
                    breadcrumb.setBrands(roots);
                    if (!roots.isEmpty()) {
                        selectNode(findSelectedOrFirst());
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.errorWithRetry(Messages.get("organization.load.failed"), this::loadTree)));
    }

    private void buildNodes() {
        roots.clear();
        if (treeDto == null || treeDto.getBrands() == null) {
            return;
        }
        for (OrganizationTreeDto.BrandNode brand : treeDto.getBrands()) {
            OrganizationNode brandNode = OrganizationNode.of(brand);
            if (brand.getChains() != null) {
                for (OrganizationTreeDto.ChainNode chain : brand.getChains()) {
                    OrganizationNode chainNode = OrganizationNode.of(chain, brandNode);
                    if (chain.getStores() != null) {
                        for (OrganizationTreeDto.StoreNode store : chain.getStores()) {
                            chainNode.addChild(OrganizationNode.of(store, chainNode));
                        }
                    }
                    brandNode.addChild(chainNode);
                }
            }
            roots.add(brandNode);
        }
    }

    private OrganizationNode findSelectedOrFirst() {
        OrganizationNode match = selectedNode == null ? null
                : findNode(roots, selectedNode.getType(), selectedNode.getId());
        return match != null ? match : roots.getFirst();
    }

    private OrganizationNode findNode(List<OrganizationNode> nodes,
                                      OrganizationNode.Type type, Integer id) {
        for (OrganizationNode node : nodes) {
            if (node.getType() == type && java.util.Objects.equals(node.getId(), id)) {
                return node;
            }
            OrganizationNode child = findNode(node.getChildren(), type, id);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private void onTreeSelected(OrganizationNode node) {
        if (syncing || node == null) {
            return;
        }
        syncing = true;
        try {
            selectedNode = node;
            breadcrumb.select(node);
            detail.show(node);
        } finally {
            syncing = false;
        }
    }

    private void onBreadcrumbSelected(OrganizationNode node) {
        if (syncing || node == null) {
            return;
        }
        syncing = true;
        try {
            selectedNode = node;
            tree.select(node);
            detail.show(node);
        } finally {
            syncing = false;
        }
    }

    private void selectNode(OrganizationNode node) {
        syncing = true;
        try {
            selectedNode = node;
            tree.select(node);
            breadcrumb.select(node);
            detail.show(node);
        } finally {
            syncing = false;
        }
    }

    private Button actionButton(String label, Runnable action) {
        Button button = new Button(label);
        button.addClickListener(event -> action.run());
        return button;
    }

    private void openEditDialog(OrganizationNode node) {
        if (node == null) {
            return;
        }
        if (node.isBrand()) {
            new BrandDialog(restClientOrganizationService, brandDtos(),
                    toBrandDto(node.getBrand()), this::loadTree).open();
        } else if (node.isChain()) {
            new ChainDialog(restClientOrganizationService, brandDtos(),
                    findBrand(node.getChain().getBrandId()), toChainDto(node.getChain()),
                    this::loadTree).open();
        } else {
            openStoreEditDialog(node.getStore());
        }
    }

    private void openStoreEditDialog(OrganizationTreeDto.StoreNode store) {
        StoreDto dto = toStoreDto(store);
        Integer brandId = brandIdForChain(store.getChainId());
        if (brandId == null) {
            openStoreEditDialog(dto, Map.of());
            return;
        }
        asyncRestClientOrganizationService.getAllTierByBrandAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> openStoreEditDialog(dto, groupTiers(result))),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.error(Messages.get("organization.store.tierLoadFailed"));
                    openStoreEditDialog(dto, Map.of());
                }),
                brandId);
    }

    private void openStoreEditDialog(StoreDto store, Map<TierTypeDto, List<TierDto>> tiers) {
        new StoreDialog(restClientOrganizationService, chainDtos(), findChain(store.getChainId()),
                store, tiers, this::openChainDialog, this::loadTree).open();
    }

    private Map<TierTypeDto, List<TierDto>> groupTiers(List<TierDto> tiers) {
        Map<TierTypeDto, List<TierDto>> grouped = new EnumMap<>(TierTypeDto.class);
        if (tiers == null) {
            return grouped;
        }
        for (TierDto tier : tiers) {
            if (tier.getType() != null) {
                grouped.computeIfAbsent(tier.getType(), key -> new ArrayList<>()).add(tier);
            }
        }
        return grouped;
    }

    private Integer brandIdForChain(Integer chainId) {
        ChainDto chain = findChain(chainId);
        return chain == null ? null : chain.getBrandId();
    }

    private BrandDto findBrand(Integer id) {
        return brandDtos().stream()
                .filter(brand -> java.util.Objects.equals(brand.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private ChainDto findChain(Integer id) {
        return chainDtos().stream()
                .filter(chain -> java.util.Objects.equals(chain.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private BrandDto toBrandDto(OrganizationTreeDto.BrandNode brand) {
        BrandDto dto = new BrandDto();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setLogoUrl(brand.getLogoUrl());
        dto.setTimeZone(brand.getTimeZone());
        dto.setCurrency(brand.getCurrency());
        dto.setAdminEmail(brand.getAdminEmail());
        dto.setInitialMenuMode(brand.getInitialMenuMode());
        dto.setSourceBrandId(brand.getSourceBrandId());
        return dto;
    }

    private ChainDto toChainDto(OrganizationTreeDto.ChainNode chain) {
        ChainDto dto = new ChainDto();
        dto.setId(chain.getId());
        dto.setName(chain.getName());
        dto.setBrandId(chain.getBrandId());
        dto.setType(chain.getType());
        dto.setManagerEmail(chain.getManagerEmail());
        dto.setTaxServiceMode(chain.getTaxServiceMode());
        dto.setTaxPercent(chain.getTaxPercent());
        dto.setServiceChargePercent(chain.getServiceChargePercent());
        return dto;
    }

    private StoreDto toStoreDto(OrganizationTreeDto.StoreNode store) {
        StoreDto dto = new StoreDto();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setChainId(store.getChainId());
        dto.setAddress(store.getAddress());
        dto.setOrderTypes(store.getOrderTypes());
        dto.setTaxServiceMode(store.getTaxServiceMode());
        dto.setTaxPercent(store.getTaxPercent());
        dto.setServiceChargePercent(store.getServiceChargePercent());
        dto.setReceiptLogoMode(store.getReceiptLogoMode());
        dto.setLogoUrl(store.getLogoUrl());
        dto.setMenuPriceMode(store.getMenuPriceMode());
        dto.setOrderTypeMode(store.getOrderTypeMode());
        dto.setTierMenuId(store.getTierMenuId());
        dto.setTierPriceId(store.getTierPriceId());
        dto.setTierServiceId(store.getTierServiceId());
        return dto;
    }

    private void openBrandDialog() {
        new BrandDialog(restClientOrganizationService, brandDtos(), this::loadTree).open();
    }

    private void openChainDialog() {
        BrandDto defaultBrand = selectedBrand();
        new ChainDialog(restClientOrganizationService, brandDtos(), defaultBrand, this::loadTree).open();
    }

    private void openStoreDialog() {
        ChainDto defaultChain = selectedChain();
        new StoreDialog(restClientOrganizationService, chainDtos(), defaultChain,
                this::openChainDialog, this::loadTree).open();
    }

    private List<BrandDto> brandDtos() {
        List<BrandDto> brands = new ArrayList<>();
        if (treeDto == null || treeDto.getBrands() == null) {
            return brands;
        }
        for (OrganizationTreeDto.BrandNode brand : treeDto.getBrands()) {
            BrandDto dto = new BrandDto();
            dto.setId(brand.getId());
            dto.setName(brand.getName());
            brands.add(dto);
        }
        return brands;
    }

    private List<ChainDto> chainDtos() {
        List<ChainDto> chains = new ArrayList<>();
        if (treeDto == null || treeDto.getBrands() == null) {
            return chains;
        }
        for (OrganizationTreeDto.BrandNode brand : treeDto.getBrands()) {
            if (brand.getChains() == null) {
                continue;
            }
            for (OrganizationTreeDto.ChainNode chain : brand.getChains()) {
                ChainDto dto = new ChainDto();
                dto.setId(chain.getId());
                dto.setName(chain.getName());
                dto.setBrandId(chain.getBrandId());
                chains.add(dto);
            }
        }
        return chains;
    }

    private BrandDto selectedBrand() {
        OrganizationNode brandNode = selectedNode;
        while (brandNode != null && !brandNode.isBrand()) {
            brandNode = brandNode.getParent();
        }
        if (brandNode != null && brandNode.getBrand() != null) {
            BrandDto dto = new BrandDto();
            dto.setId(brandNode.getBrand().getId());
            dto.setName(brandNode.getBrand().getName());
            return dto;
        }
        List<BrandDto> brands = brandDtos();
        return brands.isEmpty() ? null : brands.getFirst();
    }

    private ChainDto selectedChain() {
        OrganizationNode chainNode = selectedNode;
        while (chainNode != null && !chainNode.isChain()) {
            chainNode = chainNode.getParent();
        }
        if (chainNode != null && chainNode.getChain() != null) {
            ChainDto dto = new ChainDto();
            dto.setId(chainNode.getChain().getId());
            dto.setName(chainNode.getChain().getName());
            dto.setBrandId(chainNode.getChain().getBrandId());
            return dto;
        }
        List<ChainDto> chains = chainDtos();
        return chains.isEmpty() ? null : chains.getFirst();
    }
}
