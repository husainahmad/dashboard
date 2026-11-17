package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Right-hand card of the Users page: the selected user's profile, access and
 * timestamps, plus the edit and delete actions. Shows a calm empty state until
 * a row is selected.
 */
public class UserDetailCard extends VerticalLayout {

    private static final String DATE_TIME_PATTERN = "dd MMM yyyy HH:mm";

    private final VerticalLayout empty = new VerticalLayout();
    private final VerticalLayout content = new VerticalLayout();

    private Consumer<UserDto> editListener;
    private Consumer<UserDto> deleteListener;
    private boolean canEdit = true;
    private boolean canDelete = true;

    public UserDetailCard() {
        addClassName("user-detail-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        buildEmptyState();
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.setVisible(false);

        add(empty, content);
        setFlexGrow(1, content);
    }

    /**
     * Renders the given user, or the empty state when {@code user} is null.
     *
     * @param user      the selected user
     * @param storeName the display name of the user's store, may be null
     */
    public void show(UserDto user, String storeName) {
        show(user, storeName, null);
    }

    /**
     * Renders the given user with the roles resolved from the authentication
     * service, or the empty state when {@code user} is null.
     *
     * @param user      the selected user
     * @param storeName the display name of the user's store, may be null
     * @param roleNames the roles assigned to the user, may be null
     */
    public void show(UserDto user, String storeName, List<String> roleNames) {
        removeAll();
        if (user == null) {
            buildEmptyState();
            content.setVisible(false);
            add(empty);
            setFlexGrow(1, empty);
            return;
        }
        content.removeAll();
        content.add(header(user, storeName, roleNames), info(user, storeName, roleNames), actions(user));
        content.setVisible(true);
        add(content);
        setFlexGrow(1, content);
    }

    public void setEditListener(Consumer<UserDto> listener) {
        this.editListener = listener;
    }

    public void setDeleteListener(Consumer<UserDto> listener) {
        this.deleteListener = listener;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    private void buildEmptyState() {
        empty.removeAll();
        empty.addClassName("user-detail-empty");
        empty.setSizeFull();
        empty.setPadding(false);
        empty.setSpacing(false);
        empty.setAlignItems(Alignment.CENTER);
        empty.setJustifyContentMode(JustifyContentMode.CENTER);

        Icon icon = VaadinIcon.USER.create();
        icon.addClassName("user-detail-empty-icon");

        H3 title = new H3(Messages.get("user.detail.empty.title"));
        title.addClassName("user-detail-empty-title");

        Paragraph hint = new Paragraph(Messages.get("user.detail.empty.hint"));
        hint.addClassName("user-detail-empty-hint");

        empty.add(icon, title, hint);
    }

    private HorizontalLayout header(UserDto user, String storeName, List<String> roleNames) {
        Div avatar = new Div();
        avatar.addClassName("user-detail-avatar");
        avatar.addClassName(UserRoles.cssClass(user.getAuthId()));
        avatar.setText(UserRoles.initials(user));

        H2 name = new H2(ObjectUtils.isNotEmpty(user.getUsername()) ? user.getUsername() : "-");
        name.addClassName("user-detail-name");

        HorizontalLayout titleRow = new HorizontalLayout(name);
        if (roleNames == null || roleNames.isEmpty()) {
            Span role = new Span(UserRoles.label(user.getAuthId()));
            role.addClassName("user-role-badge");
            role.addClassName(UserRoles.cssClass(user.getAuthId()));
            titleRow.add(role);
        } else {
            for (String roleName : roleNames) {
                Span role = new Span(roleName);
                role.addClassName("user-role-badge");
                titleRow.add(role);
            }
        }
        titleRow.setAlignItems(Alignment.CENTER);
        titleRow.setSpacing(true);

        String subtitleText = ObjectUtils.isNotEmpty(user.getEmail()) ? user.getEmail()
                : ObjectUtils.isNotEmpty(storeName) ? storeName : Messages.get("user.detail.notSet");
        Span subtitle = new Span(subtitleText);
        subtitle.addClassName("user-detail-subtitle");

        VerticalLayout text = new VerticalLayout(titleRow, subtitle);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(avatar, text);
        header.addClassName("user-detail-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setFlexGrow(1, text);
        return header;
    }

    private VerticalLayout info(UserDto user, String storeName, List<String> roleNames) {
        VerticalLayout info = new VerticalLayout();
        info.addClassName("user-info-list");
        info.setPadding(false);
        info.setSpacing(false);

        info.add(infoRow(Messages.get("user.detail.store"),
                ObjectUtils.isNotEmpty(storeName) ? storeName : Messages.get("user.detail.notSet")));
        String roleText = roleNames == null || roleNames.isEmpty()
                ? UserRoles.label(user.getAuthId()) : String.join(", ", roleNames);
        info.add(infoRow(Messages.get("user.detail.role"), roleText));
        info.add(infoRow(Messages.get("user.detail.created"), formatDate(user.getCreatedAt())));
        info.add(infoRow(Messages.get("user.detail.updated"), formatDate(user.getUpdatedAt())));
        return info;
    }

    private Div infoRow(String label, String value) {
        Span name = new Span(label);
        name.addClassName("user-info-label");

        Span content = new Span(ObjectUtils.isNotEmpty(value) ? value : "-");
        content.addClassName("user-info-value");

        Div row = new Div(name, content);
        row.addClassName("user-info-row");
        return row;
    }

    private HorizontalLayout actions(UserDto user) {
        Button edit = new Button(Messages.get(Messages.Keys.ACTION_EDIT),
                event -> editListener.accept(user));
        edit.setIcon(VaadinIcon.PENCIL.create());
        edit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        edit.setVisible(canEdit);

        Button delete = new Button(Messages.get(Messages.Keys.ACTION_DELETE),
                event -> deleteListener.accept(user));
        delete.setIcon(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        delete.addClassName("user-delete-button");
        delete.setVisible(canDelete);

        HorizontalLayout actions = new HorizontalLayout(delete, edit);
        actions.addClassName("user-action-bar");
        actions.setWidthFull();
        actions.setAlignItems(Alignment.CENTER);
        actions.setJustifyContentMode(JustifyContentMode.END);
        return actions;
    }

    private static String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
