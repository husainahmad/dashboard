package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.vaadin.flow.component.tabs.Tab;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

/**
 * The per-editor state {@link ProductListView} hands to the product editor when it
 * opens a tab for a product.
 *
 * <p>These six values are not independent choices: they are the list view's current
 * brand selection, the categories and tiers fetched for it, the tab the editor was
 * opened on, and the manager that can close it. Passing them as six constructor
 * arguments meant the only thing marking which was which was its position, and the
 * two call sites - one for a new product, one for an existing one - were identical
 * for five arguments and differed in the sixth. Naming them keeps those two apart at
 * a glance and makes a swap of two same-typed neighbours impossible to miss.</p>
 *
 * <p>Deliberately separate from the two REST clients, which are Spring beans and are
 * still constructor-injected. Only the state a single editor instance needs lives
 * here; folding the clients in would make this a service locator rather than a
 * description of one editor.</p>
 *
 * <p>A record so the components are the documentation, with {@code null} collections
 * normalized to empty: every reader here iterates the lists, and a null from a
 * partially populated list view used to reach {@code setItems} and throw instead of
 * simply showing no categories.</p>
 *
 * @param brandDto        the brand the product belongs to, for customization lookups
 * @param categoryDtos    the categories to offer, already scoped to {@code brandDto}
 * @param tierDtos        the price tiers to show as columns
 * @param productTab      the tab this editor was opened on, closed after a save
 * @param productTreeItem the product being edited, or {@code null} for a new one
 * @param tabManager      closes {@code productTab} and reselects the list tab
 */
@Builder
public record ProductEditorContext(BrandDto brandDto,
                                   List<CategoryDto> categoryDtos,
                                   List<TierDto> tierDtos,
                                   Tab productTab,
                                   ProductTreeItem productTreeItem,
                                   TabManager tabManager) implements Serializable {

    public ProductEditorContext {
        categoryDtos = categoryDtos == null ? List.of() : categoryDtos;
        tierDtos = tierDtos == null ? List.of() : tierDtos;
    }

    /**
     * Whether the editor is editing a product that already exists, which decides
     * between Save and Update and between seeding from the list and loading the
     * product's details.
     *
     * <p>Spelled out here because the two forms use the test in four places, and
     * {@code productTreeItem != null} at each of them invites the reader to work out
     * again what the null is standing for.</p>
     *
     * @return {@code true} when a product was selected, {@code false} for a new one
     */
    public boolean isEditingExistingProduct() {
        return productTreeItem != null;
    }
}
