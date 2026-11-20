package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.dto.CategoryDto;
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
import java.util.function.Consumer;

/**
 * Right-hand card of the Category page: the selected category's details and
 * the edit / delete actions, or a calm empty state when nothing is selected.
 */
public class CategoryDetailCard extends VerticalLayout {

    private static final String DATE_TIME_PATTERN = "dd MMM yyyy HH:mm";

    private final VerticalLayout empty = new VerticalLayout();
    private final VerticalLayout content = new VerticalLayout();

    private Consumer<CategoryDto> editListener;
    private Consumer<CategoryDto> deleteListener;
    private boolean canEdit;
    private boolean canDelete;

    public CategoryDetailCard() {
        addClassName("category-detail-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        buildEmptyState();
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.setVisible(false);
        add(empty);
        setFlexGrow(1, empty);
    }

    /**
     * Renders the given category, or the empty state when {@code category} is null.
     *
     * @param category  the selected category
     * @param brandName the display name of the owning brand, may be null
     */
    public void show(CategoryDto category, String brandName) {
        removeAll();
        if (category == null) {
            buildEmptyState();
            content.setVisible(false);
            add(empty);
            setFlexGrow(1, empty);
            return;
        }
        content.removeAll();
        content.add(header(category, brandName), info(category, brandName), actions(category));
        content.setVisible(true);
        add(content);
        setFlexGrow(1, content);
    }

    public void setEditListener(Consumer<CategoryDto> listener) {
        this.editListener = listener;
    }

    public void setDeleteListener(Consumer<CategoryDto> listener) {
        this.deleteListener = listener;
    }

    /**
     * Sets whether the edit action is offered.
     *
     * @param canEdit {@code true} when the user may edit categories
     */
    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    /**
     * Sets whether the delete action is offered.
     *
     * @param canDelete {@code true} when the user may delete categories
     */
    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    private void buildEmptyState() {
        empty.removeAll();
        empty.addClassName("category-detail-empty");
        empty.setSizeFull();
        empty.setPadding(false);
        empty.setSpacing(false);
        empty.setAlignItems(Alignment.CENTER);
        empty.setJustifyContentMode(JustifyContentMode.CENTER);

        Icon icon = VaadinIcon.TAGS.create();
        icon.addClassName("category-detail-empty-icon");

        H3 title = new H3(Messages.get("category.detail.empty.title"));
        title.addClassName("category-detail-empty-title");

        Paragraph hint = new Paragraph(Messages.get("category.detail.empty.hint"));
        hint.addClassName("category-detail-empty-hint");

        empty.add(icon, title, hint);
    }

    private HorizontalLayout header(CategoryDto category, String brandName) {
        Div avatar = new Div(new Icon(VaadinIcon.TAGS));
        avatar.addClassName("category-detail-avatar");

        H2 name = new H2(ObjectUtils.isNotEmpty(category.getName()) ? category.getName() : "-");
        name.addClassName("category-detail-name");

        VerticalLayout titleBlock = new VerticalLayout(name, subtitle(category, brandName));
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(avatar, titleBlock);
        header.addClassName("category-detail-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setFlexGrow(1, titleBlock);
        return header;
    }

    private Span subtitle(CategoryDto category, String brandName) {
        String value = ObjectUtils.isNotEmpty(brandName) ? brandName
                : category.getBrandDto() == null ? null : category.getBrandDto().getName();
        Span subtitle = new Span(ObjectUtils.isNotEmpty(value) ? value : Messages.get("category.detail.notSet"));
        subtitle.addClassName("category-detail-subtitle");
        return subtitle;
    }

    private VerticalLayout info(CategoryDto category, String brandName) {
        VerticalLayout info = new VerticalLayout();
        info.addClassName("category-info-list");
        info.setPadding(false);
        info.setSpacing(false);

        String brand = ObjectUtils.isNotEmpty(brandName) ? brandName
                : category.getBrandDto() == null ? null : category.getBrandDto().getName();
        info.add(infoRow(Messages.get("category.detail.brand"),
                ObjectUtils.isNotEmpty(brand) ? brand : Messages.get("category.detail.notSet")));
        info.add(infoRow(Messages.get("category.detail.description"),
                ObjectUtils.isNotEmpty(category.getDescription())
                        ? category.getDescription() : Messages.get("category.detail.notSet")));
        info.add(infoRow(Messages.get("category.detail.created"), formatDate(category.getCreatedAt())));
        info.add(infoRow(Messages.get("category.detail.updated"), formatDate(category.getUpdatedAt())));
        return info;
    }

    private Div infoRow(String label, String value) {
        Span name = new Span(label);
        name.addClassName("category-info-label");

        Span content = new Span(ObjectUtils.isNotEmpty(value) ? value : "-");
        content.addClassName("category-info-value");

        Div row = new Div(name, content);
        row.addClassName("category-info-row");
        return row;
    }

    private HorizontalLayout actions(CategoryDto category) {
        Button edit = new Button(Messages.get(Messages.Keys.ACTION_EDIT),
                event -> editListener.accept(category));
        edit.setIcon(VaadinIcon.PENCIL.create());
        edit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button(Messages.get(Messages.Keys.ACTION_DELETE),
                event -> deleteListener.accept(category));
        delete.setIcon(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        delete.addClassName("category-delete-button");

        HorizontalLayout actions = new HorizontalLayout();
        if (canDelete) {
            actions.add(delete);
        }
        if (canEdit) {
            actions.add(edit);
        }
        actions.addClassName("category-action-bar");
        actions.setWidthFull();
        actions.setAlignItems(Alignment.CENTER);
        actions.setJustifyContentMode(JustifyContentMode.END);
        return actions;
    }

    private static String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
