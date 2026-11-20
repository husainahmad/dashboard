package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Map;

/**
 * Right-hand card of the Tier page: the selected tier and the settings panel
 * that matches its type.
 */
public class TierDetailCard extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final Runnable onSaved;
    private final boolean canEdit;

    private final Button saveButton = new Button(Messages.get("tier.saveChanges"));
    private final Button discardButton = new Button(Messages.get("tier.discard"));
    private final Span dirtyLabel = new Span(Messages.get("tier.unsaved"));
    private final HorizontalLayout actionBar = new HorizontalLayout();

    private TierPanel activePanel;
    private TierRow currentRow;
    private Map<Integer, List<String>> currentStoreNames;

    public TierDetailCard(AsyncRestClientMenuService asyncRestClientMenuService,
                          RestClientMenuService restClientMenuService,
                          AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                          RestClientOrganizationService restClientOrganizationService,
                          Runnable onSaved, boolean canEdit) {
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.restClientMenuService = restClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.onSaved = onSaved;
        this.canEdit = canEdit;

        addClassName("tier-detail-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        configureActionBar();
    }

    private void configureActionBar() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClassName("tier-save-button");
        saveButton.setEnabled(false);
        saveButton.addClickListener(event -> {
            if (activePanel != null) {
                activePanel.save();
            }
        });

        discardButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        discardButton.addClickListener(event -> discard());

        dirtyLabel.addClassName("tier-dirty-label");
        dirtyLabel.setVisible(false);

        actionBar.addClassName("tier-action-bar");
        actionBar.setWidthFull();
        actionBar.setAlignItems(Alignment.CENTER);
        actionBar.add(dirtyLabel, discardButton, saveButton);
        actionBar.setFlexGrow(1, dirtyLabel);
    }

    /**
     * Renders the detail for the given tier.
     *
     * @param row              the selected tier row
     * @param storeNamesByTier store names keyed by tier id
     */
    public void show(TierRow row, Map<Integer, List<String>> storeNamesByTier) {
        removeAll();
        activePanel = null;
        currentRow = row;
        currentStoreNames = storeNamesByTier;
        setDirty(false);
        if (row == null) {
            return;
        }
        TierDto tier = row.getTier();
        add(header(row, storeNamesByTier));
        activePanel = panel(tier);
        activePanel.setDirtyListener(() -> setDirty(true));
        add(activePanel);
        setFlexGrow(1, activePanel);
        if (canEdit) {
            add(actionBar);
        }
    }

    private HorizontalLayout header(TierRow row, Map<Integer, List<String>> storeNamesByTier) {
        TierDto tier = row.getTier();

        H2 name = new H2(tier.getName());
        name.addClassName("org-detail-name");

        Span typeBadge = new Span(typeLabel(tier.getType()));
        typeBadge.addClassName("tier-type-badge");
        typeBadge.addClassName("tier-type-" + typeClass(tier.getType()));

        HorizontalLayout titleRow = new HorizontalLayout(name, typeBadge);
        titleRow.setAlignItems(Alignment.CENTER);
        titleRow.setSpacing(true);

        Span count = new Span(Messages.get("tier.storeCount", row.getStoreCount()));
        count.addClassName("tier-detail-count");

        Button stores = new Button(Messages.get("tier.viewStores"), event ->
                showStores(tier, storeNamesByTier.getOrDefault(tier.getId(), List.of())));
        stores.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        stores.addClassName("tier-view-stores");

        HorizontalLayout subtitle = new HorizontalLayout(count, stores);
        subtitle.setAlignItems(Alignment.CENTER);
        subtitle.setSpacing(true);

        VerticalLayout text = new VerticalLayout(titleRow, subtitle);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(text);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.addClassName("tier-detail-header");
        header.setFlexGrow(1, text);
        return header;
    }

    private TierPanel panel(TierDto tier) {
        TierTypeDto type = tier.getType() == null ? TierTypeDto.PRICE : tier.getType();
        return switch (type) {
            case SERVICE -> new TierServicePanel(asyncRestClientOrganizationService,
                    restClientOrganizationService, tier, this::handleSaved, canEdit);
            case MENU -> new TierMenuPanel(asyncRestClientMenuService,
                    asyncRestClientOrganizationService, restClientOrganizationService, tier,
                    this::handleSaved, canEdit);
            default -> new TierPricePanel(asyncRestClientMenuService, restClientMenuService,
                    tier, this::handleSaved, canEdit);
        };
    }

    private void setDirty(boolean dirty) {
        if (!canEdit) {
            return;
        }
        saveButton.setEnabled(dirty);
        dirtyLabel.setVisible(dirty);
    }

    private void handleSaved() {
        setDirty(false);
        if (onSaved != null) {
            onSaved.run();
        }
    }

    private void discard() {
        if (currentRow != null) {
            show(currentRow, currentStoreNames);
        }
    }

    private String typeLabel(TierTypeDto type) {
        if (type == null) {
            return Messages.get("tier.type.price");
        }
        return switch (type) {
            case SERVICE -> Messages.get("tier.type.service");
            case MENU -> Messages.get("tier.type.menu");
            default -> Messages.get("tier.type.price");
        };
    }

    private String typeClass(TierTypeDto type) {
        return type == null ? "price" : type.name().toLowerCase();
    }

    private void showStores(TierDto tier, List<String> stores) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(Messages.get("tier.stores.title", tier.getName()));
        dialog.setWidth("420px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        if (stores.isEmpty()) {
            content.add(new Paragraph(Messages.get("tier.stores.empty")));
        } else {
            stores.forEach(store -> content.add(new Span(store)));
        }
        dialog.add(content);
        dialog.getFooter().add(new Button(Messages.get("action.close"), event -> dialog.close()));
        dialog.open();
    }
}
