package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Left-hand card of the Category page: search plus the category rows of the
 * selected brand.
 */
public class CategoryListCard extends VerticalLayout {

    private final Span count = new Span();
    private final TextField search = new TextField();
    private final Div loading = new Div();
    private final VerticalLayout list = new VerticalLayout();
    private final Div empty = new Div();
    private final Span summary = new Span();
    private final Map<Div, CategoryDto> rowComponents = new HashMap<>();

    private List<CategoryDto> categories = new ArrayList<>();
    private CategoryDto selected;
    private Consumer<CategoryDto> selectionListener;

    public CategoryListCard() {
        addClassName("category-list-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3(Messages.get("category.list.title"));
        title.addClassName("org-card-title");

        count.addClassName("category-list-count");
        HorizontalLayout heading = new HorizontalLayout(title, count);
        heading.setWidthFull();
        heading.setAlignItems(Alignment.CENTER);
        heading.setFlexGrow(1, title);

        search.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        search.setPlaceholder(Messages.get("category.search.placeholder"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.addValueChangeListener(event -> applyFilter());
        search.addClassName("category-list-search");

        loading.addClassName("category-list-loading");
        loading.setText(Messages.get("category.list.loading"));
        loading.setVisible(false);

        list.addClassName("category-list");
        list.setPadding(false);
        list.setSpacing(false);

        empty.addClassName("category-list-empty");
        empty.setText(Messages.get("category.list.empty"));
        empty.setVisible(false);

        summary.addClassName("category-list-summary");

        add(heading, search, loading, list, empty, summary);
        setFlexGrow(1, list);
        updateSummary();
    }

    /**
     * Replaces the categories shown by the list.
     *
     * @param categories the categories of the selected brand
     */
    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        applyFilter();
    }

    /**
     * Shows or hides the loading state.
     *
     * @param loadingState true while categories are being fetched
     */
    public void setLoading(boolean loadingState) {
        loading.setVisible(loadingState);
        applyFilter();
    }

    /**
     * Highlights the given category row.
     *
     * @param category the category to select, or {@code null} to clear
     */
    public void select(CategoryDto category) {
        this.selected = category;
        rowComponents.forEach((component, candidate) -> component.getElement().getClassList()
                .set("category-list-row-selected", sameCategory(candidate, category)));
    }

    public void setSelectionListener(Consumer<CategoryDto> listener) {
        this.selectionListener = listener;
    }

    public String getSearchTerm() {
        return search.getValue();
    }

    private void applyFilter() {
        String term = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        List<CategoryDto> filtered = categories.stream()
                .filter(category -> term.isEmpty()
                        || contains(category.getName(), term)
                        || contains(category.getDescription(), term))
                .toList();

        list.removeAll();
        rowComponents.clear();
        for (CategoryDto category : filtered) {
            Div row = rowComponent(category);
            rowComponents.put(row, category);
            list.add(row);
        }

        boolean isLoading = loading.isVisible();
        list.setVisible(!isLoading && !filtered.isEmpty());
        empty.setVisible(!isLoading && filtered.isEmpty());
        if (selected != null) {
            select(selected);
        }
        updateSummary();
    }

    private Div rowComponent(CategoryDto category) {
        Div item = new Div();
        item.addClassName("category-list-row");

        Div avatar = new Div(new Icon(VaadinIcon.TAGS));
        avatar.addClassName("category-list-avatar");

        Div text = new Div();
        text.addClassName("category-list-text");

        Span name = new Span(ObjectUtils.isNotEmpty(category.getName()) ? category.getName() : "-");
        name.addClassName("category-list-name");

        Span description = new Span(ObjectUtils.isNotEmpty(category.getDescription())
                ? category.getDescription() : Messages.get("category.list.noDescription"));
        description.addClassName("category-list-description");

        text.add(name, description);
        item.add(avatar, text);
        item.addClickListener(event -> {
            select(category);
            if (selectionListener != null) {
                selectionListener.accept(category);
            }
        });
        return item;
    }

    private void updateSummary() {
        count.setText(categories.isEmpty() ? "" : String.valueOf(categories.size()));
        count.setVisible(!loading.isVisible() && !categories.isEmpty());
        summary.setText(Messages.get("category.list.summary", categories.size()));
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private boolean sameCategory(CategoryDto left, CategoryDto right) {
        if (left == null || right == null) {
            return left == right;
        }
        return Objects.equals(left.getId(), right.getId());
    }
}
