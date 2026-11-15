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
 * Per-editor state {@link ProductListView} hands to the product editor when it
 * opens a tab: the brand and its categories/tiers, the tab the editor was opened
 * on, the product being edited ({@code null} for a new one), and the manager that
 * can close the tab. Null collections are normalized to empty so readers can
 * iterate them without null checks.
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
     * Whether the editor is editing an existing product, which decides between
     * Save and Update and between seeding from the list and loading details.
     *
     * @return {@code true} when a product was selected, {@code false} for a new one
     */
    public boolean isEditingExistingProduct() {
        return productTreeItem != null;
    }
}
