package com.harmoni.menu.dashboard.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.layout.component.StatCard;
import com.harmoni.menu.dashboard.layout.model.LowPriceRow;
import com.harmoni.menu.dashboard.layout.model.MatrixStats;
import com.harmoni.menu.dashboard.layout.model.SkuScan;
import com.harmoni.menu.dashboard.layout.model.UnpricedSku;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Landing page rendered at the root of the app after login.
 *
 * <p>Acts as a live health overview for the store's price matrix: it pulls the
 * brand's price tiers together with its products and SKUs, then reports which
 * SKUs are missing a price in one or more tiers and which SKUs carry the lowest
 * prices. The page also tracks recent admin activity (menu, tiers, products,
 * categories, brands and chains) via the {@link Broadcaster}.</p>
 */
@Slf4j
@RequiredArgsConstructor
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | POSHarmoni")
public class DashboardView extends AbstractListView {

    private static final int PRODUCT_PAGE_SIZE = 300;
    private static final String ACTIVITY_SESSION_KEY = "dashboard-activity";

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    private final LoadingBar loadingBar = new LoadingBar();
    private final GridSkeleton healthSkeleton = new GridSkeleton(5);
    private final GridSkeleton lowestSkeleton = new GridSkeleton(5);
    private final Grid<UnpricedSku> healthGrid = new Grid<>();
    private final Grid<LowPriceRow> lowestGrid = new Grid<>();
    private final Span healthSummary = new Span("Analyzing price matrix…");
    private final Span healthEmpty = new Span();
    private final VerticalLayout activityLines = new VerticalLayout();
    private final Span lowestSummary = new Span("Loading…");

    private StatCard productsCard;
    private StatCard skusCard;
    private StatCard tiersCard;
    private StatCard missingCard;

    private final AtomicInteger pendingFetches = new AtomicInteger();
    private volatile List<TierDto> priceTiers;
    private volatile List<ProductDto> products;
    private final Map<String, Date> lastActivity = new LinkedHashMap<>();

    /**
     * Builds the hero, stat cards and health panels in order.
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        addClassName("dashboard-view");
        setSizeFull();
        setPadding(false);

        add(loadingBar);
        add(createHero());
        add(createStatCards());
        add(createHealthPanel());
        add(createPricingPanel());
        add(createActivityPanel());

        loadActivity();
        trackBroadcast(Broadcaster.register(this::acceptNotification));
        renderActivityLines();

        loadingBar.start();
        loadDashboard();
    }

    private VerticalLayout createHero() {
        H2 title = new H2("Store health");
        Paragraph subtitle = new Paragraph("Live pricing and menu status for your active brand.");

        VerticalLayout hero = new VerticalLayout(title, subtitle);
        hero.addClassName("dashboard-hero");
        hero.setWidthFull();
        hero.setAlignItems(FlexComponent.Alignment.START);
        return hero;
    }

    private HorizontalLayout createStatCards() {
        productsCard = new StatCard(VaadinIcon.COFFEE, "Products");
        skusCard = new StatCard(VaadinIcon.TAGS, "SKUs");
        tiersCard = new StatCard(VaadinIcon.COINS, "Price tiers");
        missingCard = new StatCard(VaadinIcon.WARNING, "Missing prices");

        HorizontalLayout cards = new HorizontalLayout(productsCard, skusCard, tiersCard, missingCard);
        cards.addClassName("stat-cards");
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.setPadding(false);
        cards.setFlexGrow(1, productsCard, skusCard, tiersCard, missingCard);
        cards.setAlignItems(FlexComponent.Alignment.STRETCH);
        return cards;
    }

    private VerticalLayout createHealthPanel() {
        H2 title = new H2("Price matrix health");
        Button review = new Button("Review products", event -> {
            if (ui != null) {
                ui.navigate("product");
            }
        });
        review.addClassName("small-button");
        HorizontalLayout header = new HorizontalLayout(title, review);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        healthSummary.addClassName("health-caption");
        healthEmpty.addClassName("health-empty");
        healthEmpty.setVisible(false);
        healthEmpty.setText("All SKUs are priced — no missing cells.");

        healthGrid.addColumn(UnpricedSku::product).setHeader("Product").setWidth("220px").setFlexGrow(1);
        healthGrid.addColumn(UnpricedSku::sku).setHeader("SKU").setWidth("180px").setFlexGrow(1);
        healthGrid.addColumn(row -> String.join(", ", row.missingTiers()))
            .setHeader("Missing price in").setFlexGrow(1);
        healthGrid.addClassName("health-grid");
        healthGrid.setHeight("280px");
        healthGrid.setSelectionMode(Grid.SelectionMode.NONE);

        return createPanel(header, healthSummary, healthEmpty, gridSlot(healthGrid, healthSkeleton));
    }

    private VerticalLayout createPricingPanel() {
        H2 title = new H2("Lowest priced SKUs");
        lowestSummary.addClassName("health-caption");

        lowestGrid.addColumn(LowPriceRow::product).setHeader("Product").setWidth("220px").setFlexGrow(1);
        lowestGrid.addColumn(LowPriceRow::sku).setHeader("SKU").setWidth("180px").setFlexGrow(1);
        lowestGrid.addColumn(row -> UiUtil.rupiah(row.price())).setHeader("Price").setFlexGrow(1);
        lowestGrid.addColumn(LowPriceRow::tier).setHeader("Tier").setFlexGrow(1);
        lowestGrid.addClassName("health-grid");
        lowestGrid.setHeight("280px");
        lowestGrid.setSelectionMode(Grid.SelectionMode.NONE);

        return createPanel(title, lowestSummary, gridSlot(lowestGrid, lowestSkeleton));
    }

    private VerticalLayout createActivityPanel() {
        H2 title = new H2("Recent activity");
        Span caption = new Span("Changes recorded while the dashboard is open.");
        caption.addClassName("health-caption");

        activityLines.setPadding(false);
        activityLines.setSpacing(false);

        return createPanel(title, caption, activityLines);
    }

    /**
     * Creates a padded, full-width panel with the shared "panel" styling.
     *
     * @param children the panel content in order
     * @return the configured panel
     */
    private VerticalLayout createPanel(Component... children) {
        VerticalLayout panel = new VerticalLayout(children);
        panel.addClassName("panel");
        panel.setWidthFull();
        panel.setPadding(true);
        return panel;
    }

    /**
     * Runs the parallel fetches for the brand's price tiers and its products,
     * updating the individual cards and panels as each response lands.
     */
    private void loadDashboard() {
        Integer brandId = resolveBrandId();
        if (brandId == null) {
            showNoBrandState();
            return;
        }
        pendingFetches.set(2);
        healthSkeleton.show();
        lowestSkeleton.show();
        loadPriceTiers(brandId);
        loadProducts(brandId);
    }

    /**
     * Flips the dashboard into its "no active brand" state: all the panels are
     * cleared and the hero caption explains that no brand could be resolved.
     */
    private void showNoBrandState() {
        UiUtil.safeAccess(ui, () -> {
            loadingBar.stop();
            healthSkeleton.hide();
            lowestSkeleton.hide();
            skusCard.setValue(0);
            productsCard.setValue(0);
            tiersCard.setValue(0);
            missingCard.setValue(0);
            healthSummary.setText("No active brand found for this store.");
        });
    }

    /**
     * Fetches the brand's price tiers and refreshes the matrix once both it and
     * the products are available.
     *
     * @param brandId the active brand to load tiers for
     */
    private void loadPriceTiers(Integer brandId) {
        restClientMenuService.getAllTierByBrand(brandId, TierTypeDto.PRICE.toString())
            .subscribe(resp -> {
                parsePriceTiers(resp);
                if (fetchCompleted()) {
                    UiUtil.safeAccess(ui, () -> {
                        tiersCard.setValue(sizeOf(priceTiers));
                        refreshMatrix();
                    });
                }
            }, error -> {
                log.warn("Could not load price tiers", error);
                handleFetchError(healthSkeleton);
            });
    }

    /**
     * Fetches the brand's products and refreshes the matrix once both it and
     * the price tiers are available.
     *
     * @param brandId the active brand to load products for
     */
    private void loadProducts(Integer brandId) {
        asyncRestClientMenuService.getAllProductCategoryBrandAsync(result -> UiUtil.safeAccess(ui, () -> {
            parseProducts(result);
            productsCard.setValue(sizeOf(products));
            if (fetchCompleted()) {
                refreshMatrix();
            }
        }), error -> {
            log.warn("Could not load dashboard products", error);
            handleFetchError(lowestSkeleton);
        }, -1, brandId, 1, PRODUCT_PAGE_SIZE, "");
    }

    /**
     * Handles a failed parallel fetch: logs it and, once every parallel fetch
     * has landed, hides the affected skeleton and offers a retry.
     *
     * @param skeleton the panel skeleton to hide when recovery begins
     */
    private void handleFetchError(GridSkeleton skeleton) {
        if (!fetchCompleted()) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            skeleton.hide();
            UiUtil.errorWithRetry("Couldn't refresh dashboard", this::loadDashboard);
        });
    }

    /**
     * Stores the parsed price tiers from the REST response, skipping empty
     * payloads so previous data stays on screen.
     *
     * @param resp the tier response to parse
     */
    private void parsePriceTiers(RestAPIResponse resp) {
        if (ObjectUtils.isNotEmpty(resp.getData())) {
            priceTiers = ObjectUtil.convertObjectToObject(resp.getData(), new TypeReference<>() {
            });
        }
    }

    /**
     * Stores the parsed products from the REST response, skipping empty or
     * unexpected payloads so previous data stays on screen.
     *
     * @param result the product response to parse
     */
    private void parseProducts(Map<String, Object> result) {
        if (ObjectUtils.isEmpty(result.get("data")) || !(result.get("data") instanceof List<?> dataList)) {
            return;
        }
        products = new ArrayList<>();
        dataList.forEach(o -> products.add(ObjectUtil.convertValueToObject(o, ProductDto.class)));
    }

    /**
     * Marks one parallel fetch as done, stopping the loading bar when both the
     * tiers and the products have landed.
     *
     * @return whether this was the last pending fetch to complete
     */
    private boolean fetchCompleted() {
        if (pendingFetches.decrementAndGet() == 0) {
            loadingBar.stop();
            return true;
        }
        return false;
    }

    /**
     * Returns a collection's size, falling back to zero when it is {@code null}.
     *
     * @param collection the collection to size, may be {@code null}
     * @return the number of entries, or zero
     */
    private int sizeOf(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    /**
     * Resolves the active brand id from the current user's store chain, or
     * {@code null} when the chain is not set up yet.
     *
     * @return the active brand id, possibly {@code null}
     */
    private Integer resolveBrandId() {
        if (accessService.getUserDetail() == null
            || accessService.getUserDetail().getStoreDto() == null
            || accessService.getUserDetail().getStoreDto().getChainDto() == null) {
            return null;
        }
        return accessService.getUserDetail().getStoreDto().getChainDto().getBrandId();
    }

    /**
     * Recomputes the matrix health and lowest-price panels once both the tiers
     * and the products are available. Each missing price per tier is reported,
     * and every priced SKU contributes its cheapest tier price to the watchlist.
     */
    private void refreshMatrix() {
        if (priceTiers == null || products == null) {
            return;
        }
        healthSkeleton.hide();
        lowestSkeleton.hide();

        MatrixStats stats = scanMatrix();

        skusCard.setValue(stats.skuCount());
        missingCard.setValue(sizeOf(stats.unpriced()));
        missingCard.setWarn(!stats.unpriced().isEmpty());

        renderHealthView(stats);
        renderLowestView(topLowestPrices(stats.lowest()));
    }

    /**
     * Renders the price-matrix health panel from the scan: the caption, the
     * offending grid and the "all good" empty state.
     *
     * @param stats the matrix scan result
     */
    private void renderHealthView(MatrixStats stats) {
        boolean healthy = stats.unpriced().isEmpty();
        healthSummary.setText(healthy
            ? "All %d SKUs are priced in every tier.".formatted(stats.skuCount())
            : "%d of %d SKUs are missing a price in at least one tier."
                .formatted(sizeOf(stats.unpriced()), stats.skuCount()));
        healthGrid.setItems(stats.unpriced());
        healthGrid.setVisible(!healthy);
        healthEmpty.setVisible(healthy);
    }

    /**
     * Renders the lowest-priced SKUs panel from the sorted watchlist rows.
     *
     * @param top the rows to display, already sorted and capped
     */
    private void renderLowestView(List<LowPriceRow> top) {
        lowestSummary.setText(top.isEmpty()
            ? "No priced SKUs found yet."
            : "%d lowest priced SKUs across tiers.".formatted(top.size()));
        lowestGrid.setItems(top);
    }

    /**
     * Walks every product and its SKUs, flagging the SKUs missing a price in
     * at least one tier and the tier holding each SKU's cheapest price.
     *
     * @return the aggregated matrix scan result
     */
    private MatrixStats scanMatrix() {
        List<UnpricedSku> unpriced = new ArrayList<>();
        List<LowPriceRow> lowest = new ArrayList<>();
        int skuCount = 0;

        for (ProductDto product : products) {
            if (product == null || product.getSkuDtos() == null) {
                continue;
            }
            for (SkuDto sku : product.getSkuDtos()) {
                if (sku == null) {
                    continue;
                }
                skuCount++;
                SkuScan scan = scanSku(product, sku);
                if (scan.unpriced() != null) {
                    unpriced.add(scan.unpriced());
                }
                if (scan.lowest() != null) {
                    lowest.add(scan.lowest());
                }
            }
        }
        return new MatrixStats(unpriced, lowest, skuCount);
    }

    /**
     * Scans a single SKU across all price tiers, computing which tiers the
     * SKU has no price in and which tier carries its lowest price.
     *
     * @param product the SKU's product for display
     * @param sku     the SKU to scan
     * @return the unpriced tier list and cheapest tier, either may be absent
     */
    private SkuScan scanSku(ProductDto product, SkuDto sku) {
        Map<Integer, Double> byTier = tierPriceMap(sku);
        List<String> missingTiers = new ArrayList<>();
        double minPrice = Double.MAX_VALUE;
        String minTier = null;

        for (TierDto tier : priceTiers) {
            Double price = byTier.get(tier.getId());
            if (price == null) {
                missingTiers.add(tier.getName());
            } else if (price < minPrice) {
                minPrice = price;
                minTier = tier.getName();
            }
        }

        UnpricedSku unpriced = missingTiers.isEmpty() ? null
                : new UnpricedSku(product.getName(), sku.getName(), missingTiers);
        LowPriceRow lowest = minTier == null ? null
                : new LowPriceRow(product.getName(), sku.getName(), minPrice, minTier);
        return new SkuScan(unpriced, lowest);
    }

    /**
     * Returns the cheapest rows to show in the watchlist, sorted ascending by
     * price and capped at the panel's maximum of eight entries.
     *
     * @param lowest all priced SKU rows found by the scan
     * @return the sorted rows to display
     */
    private List<LowPriceRow> topLowestPrices(List<LowPriceRow> lowest) {
        lowest.sort(Comparator.comparingDouble(LowPriceRow::price));
        return lowest.size() <= 8 ? lowest : lowest.subList(0, 8);
    }

    private Map<Integer, Double> tierPriceMap(SkuDto sku) {
        Map<Integer, Double> byTier = new HashMap<>();
        if (sku.getSkuTierPriceDtos() == null) {
            return byTier;
        }
        for (SkuTierPriceDto row : sku.getSkuTierPriceDtos()) {
            if (row != null && row.getTierId() != null) {
                byTier.put(row.getTierId(), row.getPrice());
            }
        }
        return byTier;
    }

    /**
     * Responds to admin change broadcasts, stamping the affected area with the
     * current time and reflecting it in the activity panel.
     */
    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            String label = activityLabel(broadcastMessage.getType());
            if (label == null || ui == null) {
                return;
            }
            UiUtil.safeAccess(ui, () -> {
                lastActivity.put(label, new Date());
                saveActivity();
                renderActivityLines();
            });
        } catch (JsonProcessingException e) {
            log.warn("Could not parse dashboard broadcast message", e);
        }
    }

    private String activityLabel(String type) {
        return switch (type) {
            case BroadcastMessage.PRODUCT_INSERT_SUCCESS, BroadcastMessage.PRODUCT_UPDATE_SUCCESS -> "Products";
            case BroadcastMessage.CATEGORY_INSERT_SUCCESS, BroadcastMessage.CATEGORY_UPDATED_SUCCESS -> "Categories";
            case BroadcastMessage.TIER_INSERT_SUCCESS, BroadcastMessage.TIER_UPDATED_SUCCESS,
                BroadcastMessage.TIER_DELETED_SUCCESS -> "Tiers";
            case BroadcastMessage.BRAND_INSERT_SUCCESS, BroadcastMessage.BRAND_SUCCESS_UPDATED -> "Brands";
            case BroadcastMessage.CHAIN_INSERT_SUCCESS, BroadcastMessage.CHAIN_SUCCESS_UPDATED -> "Chains";
            default -> null;
        };
    }

    private void renderActivityLines() {
        activityLines.removeAll();
        if (lastActivity.isEmpty()) {
            activityLines.add(new Span("No changes recorded yet."));
            return;
        }
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        lastActivity.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
            .forEach(entry -> activityLines.add(new Span(
                timeFormat.format(entry.getValue()) + "  ·  " + entry.getKey())));
    }

    private void loadActivity() {
        if (ui == null || ui.getSession() == null) {
            return;
        }
        Object stored = ui.getSession().getAttribute(ACTIVITY_SESSION_KEY);
        if (stored instanceof Map<?, ?> map) {
            lastActivity.clear();
            map.forEach((key, value) -> {
                if (key instanceof String k && value instanceof Date d) {
                    lastActivity.put(k, d);
                }
            });
        }
    }

    private void saveActivity() {
        if (ui != null && ui.getSession() != null) {
            ui.getSession().setAttribute(ACTIVITY_SESSION_KEY, new LinkedHashMap<>(lastActivity));
        }
    }
}