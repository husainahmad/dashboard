package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Vaadin grid view listing the customers of the customer service with
 * pagination and a free-text filter over name, phone and email.
 *
 * <p>Rows open the read-only {@link CustomerDetailView} in a new tab through
 * {@link TabManager}; create, edit and delete actions are intentionally left
 * out until the corresponding customer endpoints are wired into the
 * dashboard.</p>
 *
 * <p>The dashboard paging state is one-based while the customer service pages
 * are zero-based, so the page index is converted on the way out.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class CustomerListView extends AbstractListView {

    /** Rows fetched per page. */
    private static final int PAGE_SIZE = 10;

    private static final String DATE_TIME_PATTERN = "dd MMM yyyy HH:mm";

    private final Grid<CustomerDto> customerDtoGrid = new Grid<>(CustomerDto.class);

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;

    private final GridSkeleton gridSkeleton = new GridSkeleton(PAGE_SIZE);

    /**
     * Renders the grid layout with the customer list and pagination footer. The
     * toolbar is added by the hosting {@link CustomerTabs} component.
     */
    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();
        add(getContent(), getPaginationFooter());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderLayout();
        fetchCustomers();
    }

    /**
     * Configures the customer grid with columns for name, phone, email, the
     * creation date and a detail action. Sets the grid to full size and defines
     * the empty state text.
     */
    private void configureGrid() {
        customerDtoGrid.setSizeFull();
        customerDtoGrid.removeAllColumns();
        customerDtoGrid.setEmptyStateText(Messages.get("grid.empty.customers"));
        customerDtoGrid.addColumn(CustomerDto::getName).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));
        customerDtoGrid.addColumn(CustomerDto::getPhone)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_PHONE));
        customerDtoGrid.addColumn(CustomerDto::getEmail)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_EMAIL));
        customerDtoGrid.addColumn(customerDto -> formatDate(customerDto.getCreatedAt()))
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_CREATED_AT));
        customerDtoGrid.addComponentColumn(this::applyDetailButton)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        customerDtoGrid.getColumns().forEach(column -> column.setAutoWidth(true));
    }

    /**
     * Formats a timestamp for the grid, or {@code -} when it is not set.
     *
     * @param date the timestamp to format
     * @return the formatted timestamp
     */
    private static String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }

    /**
     * Creates a horizontal layout holding the button that opens the customer
     * detail tab.
     *
     * @param customerDto the customer to open
     * @return a horizontal layout containing the detail button
     */
    private Component applyDetailButton(CustomerDto customerDto) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.add(UiUtil.viewButton(Messages.get(Messages.Keys.ACTION_VIEW_DETAIL),
                event -> showCustomerDetail(customerDto)));
        return actions;
    }

    /**
     * Opens the read-only detail of the given customer in a tab, reusing the
     * tab when it is already open.
     *
     * @param customerDto the customer to show
     */
    private void showCustomerDetail(CustomerDto customerDto) {
        if (customerDto == null || customerDto.getId() == null
                || !(getParent().orElse(null) instanceof TabSheet tabSheet)) {
            return;
        }
        String tabLabel = Messages.get(Messages.Keys.ACTION_VIEW_NAME, customerDto.getName());
        new TabManager(tabSheet).addOrSelect(tabLabel,
                tab -> new CustomerDetailView(asyncRestClientCustomerService, customerDto.getId()));
    }

    /**
     * Creates a horizontal layout containing the customer grid and a skeleton
     * loader.
     *
     * @return a horizontal layout with the grid and skeleton
     */
    private HorizontalLayout getContent() {
        return gridSlot(customerDtoGrid, gridSkeleton);
    }

    /**
     * Builds the toolbar with a lazy filter matching name, phone and email.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();
        filterText.setPlaceholder(Messages.get("placeholder.filterCustomer"));
        filterText.addValueChangeListener(changeEvent -> {
            if (!changeEvent.getOldValue().equals(changeEvent.getValue())) {
                currentPage = 1;
                fetchCustomers();
            }
        });

        registerSearchShortcut();

        HorizontalLayout toolbar = new HorizontalLayout(filterText);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setFlexGrow(1, filterText);
        return toolbar;
    }

    /**
     * Registers the {@code /} shortcut focusing the search filter. The shared
     * {@code n} shortcut is intentionally left out because the customer list is
     * read-only for now.
     */
    private void registerSearchShortcut() {
        Shortcuts.addShortcutListener(this, filterText::focus, Key.SLASH);
    }

    /**
     * Creates a pagination footer with previous and next buttons. The buttons
     * update the current page and fetch customers accordingly.
     *
     * @return a horizontal layout containing the pagination controls
     */
    private HorizontalLayout getPaginationFooter() {
        return paginationFooter(() -> {
            if (currentPage > 1) {
                currentPage--;
                fetchCustomers();
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchCustomers();
            }
        });
    }

    /**
     * Fetches the current page of customers from the customer service
     * asynchronously. Updates the grid, keeps the pagination footer in sync and
     * shows an error notification with a retry action on failure.
     */
    private void fetchCustomers() {
        gridSkeleton.show();
        asyncRestClientCustomerService.searchCustomersAsync(result -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            if (result == null) {
                customerDtoGrid.setItems(new ArrayList<>());
                totalPages = 0;
                updatePagination();
                return;
            }
            List<CustomerDto> customers = result.getContent() == null
                    ? new ArrayList<>() : result.getContent();
            customerDtoGrid.setItems(customers);
            totalPages = result.getTotalPages();
            if (currentPage > totalPages && totalPages > 0) {
                currentPage = totalPages;
            }
            updatePagination();
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get("notification.customer.loadFailed"), this::fetchCustomers);
        }), filterText.getValue(), currentPage - 1, PAGE_SIZE);
    }
}
