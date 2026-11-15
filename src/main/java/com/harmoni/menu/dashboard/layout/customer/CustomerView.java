package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.dto.CustomerPageDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
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
 * Unified Customer page: a server-side search over the chain's customers on the
 * left and the selected customer's masked profile on the right. Customer records
 * are created at the POS, so the page is read-only.
 */
public class CustomerView extends VerticalLayout {

    private static final int PAGE_SIZE = 10;

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;

    private final CustomerListCard listCard = new CustomerListCard();
    private final CustomerDetailCard detailCard = new CustomerDetailCard();
    private final TextField search = new TextField();

    private List<CustomerDto> pageCustomers = new ArrayList<>();
    private CustomerDto selectedCustomer;
    private int currentPage = 1;
    private int totalPages;
    private long totalCustomers;
    private boolean loaded;

    public CustomerView(AsyncRestClientCustomerService asyncRestClientCustomerService) {
        this.asyncRestClientCustomerService = asyncRestClientCustomerService;

        addClassName("customer-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        PageHeader header = PageHeader.of("page.customer.title", "page.customer.description");

        search.setPlaceholder(Messages.get("customer.search.placeholder"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.addValueChangeListener(event -> {
            if (!Objects.equals(event.getOldValue(), event.getValue())) {
                currentPage = 1;
                fetchCustomers();
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(search);
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
        if (!loaded) {
            loaded = true;
            Shortcuts.addShortcutListener(this, search::focus, Key.SLASH);
        }
        fetchCustomers();
    }

    private void fetchCustomers() {
        listCard.setLoading(true);
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
                search.getValue(), currentPage - 1, PAGE_SIZE);
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
}
