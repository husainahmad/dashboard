package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.event.promotion.PromotionDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientPromotionService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The "All Promotions" browser inside {@link PromotionTabs}. A rich table keeps
 * the code, name, type, status, schedule and priority scannable in one row while
 * the editor still opens in a tab.
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionListView extends AbstractListView {

    private static final int PAGE_SIZE = 15;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final AsyncRestClientPromotionService asyncRestClientPromotionService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientPromotionService restClientPromotionService;
    private final AccessService accessService;

    private final Grid<PromotionDto> promotionGrid = new Grid<>(PromotionDto.class);
    private final GridSkeleton gridSkeleton = new GridSkeleton(PAGE_SIZE);
    private final AtomicInteger requestGeneration = new AtomicInteger();

    private final ComboBox<PromotionStatus> statusFilter = new ComboBox<>();
    private final ComboBox<PromotionType> typeFilter = new ComboBox<>();
    private final Span count = new Span();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.PROMOTION_INSERT_SUCCESS,
                BroadcastMessage.PROMOTION_UPDATED_SUCCESS,
                BroadcastMessage.PROMOTION_STATUS_UPDATED_SUCCESS,
                BroadcastMessage.PROMOTION_DELETE_SUCCESS), this::fetchPromotions);
        buildLayout();
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        configureSearch();
        configureFilters();
        configureGrid();

        H3 title = new H3(Messages.get("promotion.list.title"));
        title.addClassName("org-card-title");

        count.addClassName("promotion-list-count");
        HorizontalLayout header = new HorizontalLayout(title, count);
        header.addClassName("promotion-list-header");
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setFlexGrow(1, title);

        HorizontalLayout content = getContent();
        add(header, content, getPaginationFooter());
        setFlexGrow(1, content);

        fetchPromotions();
    }

    private void configureGrid() {
        promotionGrid.setSizeFull();
        promotionGrid.setEmptyStateText(Messages.get("promotion.list.empty"));
        promotionGrid.removeAllColumns();

        promotionGrid.addComponentColumn(this::codeCell)
                .setHeader(Messages.get("grid.header.code"))
                .setAutoWidth(true)
                .setFlexGrow(0);
        promotionGrid.addColumn(promotion -> orDash(promotion.getName()))
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME))
                .setFlexGrow(1);
        promotionGrid.addComponentColumn(this::typeBadge)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_TYPE))
                .setAutoWidth(true);
        promotionGrid.addComponentColumn(this::statusBadge)
                .setHeader(Messages.get("grid.header.status"))
                .setAutoWidth(true);
        promotionGrid.addColumn(this::describeWindow)
                .setHeader(Messages.get("grid.header.dateRange"))
                .setAutoWidth(true);
        promotionGrid.addColumn(promotion -> promotion.getPriority() == null
                        ? "0" : String.valueOf(promotion.getPriority()))
                .setHeader(Messages.get("grid.header.priority"))
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        promotionGrid.addComponentColumn(this::applyActionButtons)
                .setHeader(Messages.get("grid.header.actions"))
                .setAutoWidth(true);

        promotionGrid.setPartNameGenerator(promotion -> promotion.getStatus() == null
                ? null : "promotion-row-" + promotion.getStatus().getMessageKey());
        promotionGrid.getColumns().forEach(column -> column.setResizable(true));
    }

    private Component codeCell(PromotionDto promotion) {
        Span code = new Span(orDash(promotion.getCode()));
        code.addClassName("promotion-table-code");
        return code;
    }

    private Component typeBadge(PromotionDto promotion) {
        Span badge = new Span(promotion.getPromotionType() == null
                ? "-" : promotion.getPromotionType().getLabel());
        badge.addClassName("promotion-type");
        if (promotion.getPromotionType() != null) {
            badge.addClassName("promotion-type-" + promotion.getPromotionType().getMessageKey());
        }
        return badge;
    }

    private Component statusBadge(PromotionDto promotion) {
        Span badge = new Span(promotion.getStatus() == null
                ? "-" : promotion.getStatus().getLabel());
        badge.addClassName("promotion-status");
        if (promotion.getStatus() != null) {
            badge.addClassName("promotion-status-" + promotion.getStatus().getMessageKey());
        }
        return badge;
    }

    private Component applyActionButtons(PromotionDto promotion) {
        Button editButton = UiUtil.editButton(Messages.get(Messages.Keys.ACTION_EDIT),
                event -> openForm(promotion, FormAction.EDIT));
        Button statusButton = new Button(VaadinIcon.PLAY.create());
        statusButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        statusButton.getElement().setAttribute("title", Messages.get("action.applyStatus"));
        statusButton.addClickListener(event -> openStatusTransition(promotion));
        Button deleteButton = UiUtil.deleteButton(
                new PromotionDeleteEventListener(promotion, restClientPromotionService));
        HorizontalLayout actions = new HorizontalLayout(editButton, statusButton, deleteButton);
        actions.setSpacing(false);
        return actions;
    }

    private String describeWindow(PromotionDto promotion) {
        String from = promotion.getStartDate() == null ? "?" : DATE_FORMAT.format(promotion.getStartDate());
        String to = promotion.getEndDate() == null ? "?" : DATE_FORMAT.format(promotion.getEndDate());
        return from.concat("  -  ").concat(to);
    }

    private String orDash(String value) {
        return ObjectUtils.isEmpty(value) ? "-" : value;
    }

    private void configureSearch() {
        filterText.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        configureSearchFilter();
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchPromotions();
            }
        });
    }

    private void configureFilters() {
        statusFilter.setLabel(Messages.get("label.promotion.status"));
        statusFilter.setItemLabelGenerator(PromotionStatus::getLabel);
        statusFilter.setItems(PromotionStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchPromotions();
            }
        });

        typeFilter.setLabel(Messages.get("label.promotion.type"));
        typeFilter.setItemLabelGenerator(PromotionType::getLabel);
        typeFilter.setItems(PromotionType.values());
        typeFilter.setClearButtonVisible(true);
        typeFilter.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchPromotions();
            }
        });
    }

    private HorizontalLayout getContent() {
        return gridSlot(promotionGrid, gridSkeleton);
    }

    private HorizontalLayout getPaginationFooter() {
        return paginationFooter(() -> {
            if (currentPage > 1) {
                currentPage--;
                fetchPromotions();
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchPromotions();
            }
        });
    }

    /**
     * Builds the list toolbar: the status and type selectors, the search filter and
     * the "New Promotion" button.
     *
     * @return the toolbar row for this list view
     */
    public HorizontalLayout getToolbarComponent() {
        Button addButton = UiUtil.addButton(Messages.get("action.newPromotion"), event -> {
            openForm(null, FormAction.CREATE);
        });
        HorizontalLayout toolbar = new HorizontalLayout(statusFilter, typeFilter, filterText, addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        registerNewShortcut(() -> openForm(null, FormAction.CREATE));
        return toolbar;
    }

    private void openForm(PromotionDto promotion, FormAction formAction) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        if (formAction == FormAction.CREATE || ObjectUtils.isEmpty(promotion.getId())) {
            String label = Messages.get("tab.promotionNew");
            tabManager.addOrSelect(label, tab -> new PromotionFormWithPreview(
                    restClientPromotionService, asyncRestClientMenuService,
                    asyncRestClientOrganizationService,
                    PromotionEditorContext.builder()
                            .tabManager(tabManager)
                            .currentTab(tab)
                            .formAction(formAction)
                            .build(),
                    accessService));
            return;
        }
        restClientPromotionService.getPromotionById(promotion.getId()).subscribe(response -> {
            if (ObjectUtils.isEmpty(response.getData()) || ui == null) {
                return;
            }
            PromotionDto detail = ObjectUtil.convertValueToObject(response.getData(), PromotionDto.class);
            UiUtil.safeAccess(ui, () -> {
                TabManager manager = new TabManager(tabSheet);
                String label = ObjectUtils.isEmpty(detail.getName())
                        ? Messages.get("tab.promotionEdit")
                        : Messages.get(Messages.Keys.ACTION_EDIT_NAME, detail.getName());
                manager.addOrSelect(label, tab -> new PromotionFormWithPreview(
                        restClientPromotionService, asyncRestClientMenuService,
                        asyncRestClientOrganizationService,
                        PromotionEditorContext.builder()
                                .tabManager(manager)
                                .currentTab(tab)
                                .formAction(FormAction.EDIT)
                                .promotionDto(detail)
                                .build(),
                        accessService));
            });
        }, error -> log.error("Failed to load promotion detail id={}", promotion.getId(), error));
    }

    private void openStatusTransition(PromotionDto promotion) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        String label = ObjectUtils.isEmpty(promotion.getName())
                ? Messages.get("tab.promotionStatus")
                : Messages.get("tab.promotionStatusNamed", promotion.getName());
        tabManager.addOrSelect(label, tab -> new PromotionFormWithPreview(
                restClientPromotionService, asyncRestClientMenuService, asyncRestClientOrganizationService,
                PromotionEditorContext.builder()
                        .tabManager(tabManager)
                        .currentTab(tab)
                        .formAction(FormAction.STATUS)
                        .promotionDto(promotion)
                        .build(),
                accessService));
    }

    private void fetchPromotions() {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            int generation = requestGeneration.incrementAndGet();
            gridSkeleton.show();
            PromotionStatus status = statusFilter.getValue();
            PromotionType type = typeFilter.getValue();
            asyncRestClientPromotionService.getPromotionsAsync(
                    result -> UiUtil.safeAccess(ui, () -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        gridSkeleton.hide();
                        applyPromotions(result);
                    }),
                    error -> UiUtil.safeAccess(ui, () -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        gridSkeleton.hide();
                        UiUtil.errorWithRetry(
                                Messages.get("notification.promotion.loadFailed"), this::fetchPromotions);
                    }),
                    currentPage, PAGE_SIZE,
                    status == null ? null : status.name(),
                    type == null ? null : type.name(),
                    normalizeSearch(filterText.getValue()));
        });
    }

    private void applyPromotions(Map<String, Object> result) {
        List<PromotionDto> promotions = new ArrayList<>();
        int total = 0;
        if (result != null && result.get("data") instanceof List<?> list && !list.isEmpty()) {
            list.forEach(object -> promotions.add(
                    ObjectUtil.convertValueToObject(object, PromotionDto.class)));
            totalPages = result.get("page") == null ? 0 : Integer.parseInt(result.get("page").toString());
            total = result.get("total") == null ? promotions.size()
                    : Integer.parseInt(result.get("total").toString());
        } else {
            totalPages = 0;
        }
        promotionGrid.setItems(promotions);
        count.setText(total <= 0 ? "" : String.valueOf(total));
        count.setVisible(total > 0);
        updatePagination();
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim();
    }
}
