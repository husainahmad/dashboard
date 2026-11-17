package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.dto.CustomerPageDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Unified Customer page: a brand filter and server-side search over the brand's
 * customers on the left, and the selected customer's profile on the right.
 * Customer records are created at the POS, so the page is read-only.
 */
public class CustomerView extends VerticalLayout {

    private static final int PAGE_SIZE = 10;

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AccessService accessService;

    private final CustomerListCard listCard = new CustomerListCard();
    private final CustomerDetailCard detailCard = new CustomerDetailCard();
    private final ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_BRAND));
    private final TextField search = new TextField();

    private List<CustomerDto> pageCustomers = new ArrayList<>();
    private CustomerDto selectedCustomer;
    private int currentPage = 1;
    private int totalPages;
    private long totalCustomers;
    private boolean shortcutsWired;
    private boolean brandsLoaded;

    public CustomerView(AsyncRestClientCustomerService asyncRestClientCustomerService,
                        AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                        AccessService accessService) {
        this.asyncRestClientCustomerService = asyncRestClientCustomerService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.accessService = accessService;

        addClassName("customer-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        PageHeader header = PageHeader.of("page.customer.title", "page.customer.description");

        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setWidth("240px");
        brandBox.setClearButtonVisible(false);
        brandBox.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                currentPage = 1;
                fetchCustomers();
            }
        });

        search.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        search.setPlaceholder(Messages.get("customer.search.placeholder"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(event -> {
            if (!Objects.equals(event.getOldValue(), event.getValue())) {
                currentPage = 1;
                fetchCustomers();
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(brandBox, search);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.addClassName("customer-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        HorizontalLayout body = new HorizontalLayout(listCard, detailCard);
        body.addClassName("customer-body");
        body.setWidthFull();
        body.setSpacing(true);

        add(header, toolbar, body);
        setFlexGrow(1, body);

        listCard.setSelectionListener(this::selectCustomer);
        listCard.setPreviousListener(() -> {
            if (currentPage > 1) {
                currentPage--;
                fetchCustomers();
            }
        });
        listCard.setNextListener(() -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchCustomers();
            }
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!shortcutsWired) {
            shortcutsWired = true;
            Shortcuts.addShortcutListener(this, search::focus, Key.SLASH);
        }
        if (brandsLoaded) {
            fetchCustomers();
        } else {
            loadBrands();
        }
    }

    private void loadBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    ScopeService.ScopeSnapshot scope = scope();
                    List<BrandDto> allowed = scope == null ? result : scope.filterBrands(result);
                    if (allowed == null || allowed.isEmpty()) {
                        brandsLoaded = true;
                        fetchCustomers();
                        return;
                    }
                    brandBox.setItems(allowed);
                    brandBox.setEnabled(scope == null ? allowed.size() > 1
                            : scope.canSelectBrand(allowed.size()));
                    brandsLoaded = true;
                    brandBox.setValue(scope == null ? allowed.getFirst() : scope.defaultBrand(allowed));
                    fetchCustomers();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED),
                                this::loadBrands)));
    }

    private void fetchCustomers() {
        listCard.setLoading(true);
        Integer brandId = brandBox.getValue() == null ? null : brandBox.getValue().getId();
        asyncRestClientCustomerService.searchCustomersAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    pageCustomers = new ArrayList<>();
                    totalPages = 0;
                    totalCustomers = 0;
                    if (result != null) {
                        applyResult(result);
                    }
                    listCard.setCustomers(pageCustomers);
                    listCard.setPageInfo(currentPage, totalPages, totalCustomers);

                    CustomerDto target = findSelected();
                    if (target == null && !pageCustomers.isEmpty()) {
                        target = pageCustomers.getFirst();
                    }
                    selectCustomer(target);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    UiUtil.errorWithRetry(Messages.get("notification.customer.loadFailed"),
                            this::fetchCustomers);
                }),
                search.getValue(), brandId, currentPage - 1, PAGE_SIZE);
    }

    private void applyResult(CustomerPageDto result) {
        List<CustomerDto> content = result.getContent();
        if (content != null) {
            pageCustomers.addAll(content);
        }
        totalPages = result.getTotalPages();
        totalCustomers = result.getTotalElements();
        if (totalPages > 0 && currentPage > totalPages) {
            currentPage = totalPages;
        }
    }

    private CustomerDto findSelected() {
        Long selectedId = selectedCustomer == null ? null : selectedCustomer.getId();
        if (selectedId == null) {
            return null;
        }
        return pageCustomers.stream()
                .filter(customer -> Objects.equals(customer.getId(), selectedId))
                .findFirst()
                .orElse(null);
    }

    private void selectCustomer(CustomerDto customer) {
        selectedCustomer = customer;
        listCard.select(customer);
        detailCard.show(customer);
    }

    private static ScopeService.ScopeSnapshot scope() {
        ScopeService service = ScopeService.current();
        return service == null ? null : service.snapshot();
    }
}
