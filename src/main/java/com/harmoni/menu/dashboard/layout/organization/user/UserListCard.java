package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Left-hand card of the Users page: the current page of users with their role
 * and store, plus the pager. Search lives in the page toolbar so it can hit the
 * server-side filter.
 */
public class UserListCard extends VerticalLayout {

    private final Span count = new Span();
    private final Div loading = new Div();
    private final VerticalLayout list = new VerticalLayout();
    private final Div empty = new Div();
    private final Span summary = new Span();
    private final Button previous = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button next = new Button(VaadinIcon.ARROW_RIGHT.create());
    private final Map<Div, UserDto> rowComponents = new HashMap<>();

    private List<UserDto> users = new ArrayList<>();
    private Map<Integer, String> storeNames = new HashMap<>();
    private UserDto selected;
    private int currentPage = 1;
    private int totalPages;
    private int totalUsers;

    private Consumer<UserDto> selectionListener;
    private Runnable previousListener;
    private Runnable nextListener;

    public UserListCard() {
        addClassName("user-list-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3(Messages.get("user.list.title"));
        title.addClassName("org-card-title");

        count.addClassName("user-list-count");
        HorizontalLayout heading = new HorizontalLayout(title, count);
        heading.setWidthFull();
        heading.setAlignItems(Alignment.CENTER);
        heading.setFlexGrow(1, title);

        loading.addClassName("user-list-loading");
        loading.setText(Messages.get("user.list.loading"));
        loading.setVisible(false);

        list.addClassName("user-list");
        list.setPadding(false);
        list.setSpacing(false);

        empty.addClassName("user-list-empty");
        empty.setText(Messages.get("user.list.empty"));
        empty.setVisible(false);

        summary.addClassName("user-list-summary");

        previous.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        previous.getElement().setAttribute("title", Messages.get("action.previous"));
        previous.addClickListener(event -> {
            if (previousListener != null) {
                previousListener.run();
            }
        });

        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        next.getElement().setAttribute("title", Messages.get("action.next"));
        next.addClickListener(event -> {
            if (nextListener != null) {
                nextListener.run();
            }
        });

        HorizontalLayout footer = new HorizontalLayout(summary, previous, next);
        footer.addClassName("user-list-footer");
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.setFlexGrow(1, summary);

        add(heading, loading, list, empty, footer);
        setFlexGrow(1, list);
        updatePager();
    }

    /**
     * Replaces the visible users and the store names used by the row subtitle.
     *
     * @param users      the users of the current page
     * @param storeNames store names keyed by store id
     */
    public void setUsers(List<UserDto> users, Map<Integer, String> storeNames) {
        this.users = users == null ? new ArrayList<>() : new ArrayList<>(users);
        this.storeNames = storeNames == null ? new HashMap<>() : storeNames;
        renderRows();
    }

    /**
     * Updates the pager labels and button state.
     *
     * @param currentPage the one-based current page
     * @param totalPages  the number of pages, zero when empty
     * @param totalUsers  the total number of users across all pages
     */
    public void setPageInfo(int currentPage, int totalPages, int totalUsers) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalUsers = totalUsers;
        updatePager();
    }

    /**
     * Shows or hides the loading state.
     *
     * @param loadingState true while a page is being fetched
     */
    public void setLoading(boolean loadingState) {
        loading.setVisible(loadingState);
        updatePager();
        renderRows();
    }

    /**
     * Selects the given user and highlights its row.
     *
     * @param user the user to select, or {@code null} to clear
     */
    public void select(UserDto user) {
        this.selected = user;
        rowComponents.forEach((component, candidate) -> component.getElement().getClassList()
                .set("user-list-row-selected", sameUser(candidate, user)));
    }

    public void setSelectionListener(Consumer<UserDto> listener) {
        this.selectionListener = listener;
    }

    public void setPreviousListener(Runnable listener) {
        this.previousListener = listener;
    }

    public void setNextListener(Runnable listener) {
        this.nextListener = listener;
    }

    private void renderRows() {
        list.removeAll();
        rowComponents.clear();
        for (UserDto user : users) {
            Div row = rowComponent(user);
            rowComponents.put(row, user);
            list.add(row);
        }
        boolean isLoading = loading.isVisible();
        list.setVisible(!isLoading && !users.isEmpty());
        empty.setVisible(!isLoading && users.isEmpty());
        if (selected != null) {
            select(selected);
        }
    }

    private Div rowComponent(UserDto user) {
        Div item = new Div();
        item.addClassName("user-list-row");

        Div avatar = new Div();
        avatar.addClassName("user-list-avatar");
        avatar.addClassName(UserRoles.cssClass(user.getAuthId()));
        avatar.setText(UserRoles.initials(user));

        Div text = new Div();
        text.addClassName("user-list-text");

        Span name = new Span(ObjectUtils.isNotEmpty(user.getUsername())
                ? user.getUsername() : "-");
        name.addClassName("user-list-name");

        Span store = new Span(storeLabel(user));
        store.addClassName("user-list-store");

        text.add(name, store);

        Span role = new Span(UserRoles.label(user.getAuthId()));
        role.addClassName("user-role-badge");
        role.addClassName(UserRoles.cssClass(user.getAuthId()));

        item.add(avatar, text, role);
        item.addClickListener(event -> {
            select(user);
            if (selectionListener != null) {
                selectionListener.accept(user);
            }
        });
        return item;
    }

    private String storeLabel(UserDto user) {
        String storeName = user.getStoreId() == null ? null : storeNames.get(user.getStoreId());
        return ObjectUtils.isNotEmpty(storeName) ? storeName : Messages.get("user.list.noStore");
    }

    private void updatePager() {
        int pages = Math.max(totalPages, 1);
        summary.setText(Messages.get("user.list.summary", currentPage, pages, totalUsers));
        previous.setEnabled(currentPage > 1);
        next.setEnabled(currentPage < totalPages);
        count.setText(totalUsers <= 0 ? "" : String.valueOf(totalUsers));
        count.setVisible(!loading.isVisible() && totalUsers > 0);
    }

    private boolean sameUser(UserDto left, UserDto right) {
        if (left == null || right == null) {
            return left == right;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        return left == right;
    }
}
