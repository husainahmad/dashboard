package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;

/**
 * The product editor: a {@link ProductForm} beside a live {@link ProductPreview}.
 *
 * <p>Column sizing lives in the stylesheet, not in Java, so the single-column
 * fallback stays reachable on narrow screens. Not a route: the editor is built by
 * {@link ProductListView} inside its tab and needs the tab it was opened on, so it
 * is constructed directly. The action bar stays inside {@link ProductForm}, which
 * owns the binder-driven Save/Update state.</p>
 */
@Getter
public class ProductFormWithPreview extends VerticalLayout {

    private final ProductForm productForm;

    private final ProductPreview productPreview;

    public ProductFormWithPreview(RestClientMenuService restClientMenuService,
                                  AsyncRestClientMenuService asyncRestClientMenuService,
                                  ProductEditorContext context) {
        this.productForm = new ProductForm(restClientMenuService, asyncRestClientMenuService, context);
        this.productPreview = new ProductPreview();

        // Attached before the layout is built, so the preview is never briefly blank
        // beside an already-populated form on the first paint.
        productForm.setPreview(productPreview);
        buildLayout();
    }

    /**
     * Builds the two-column layout with the form on the left and the preview on the right.
     * <p>
     * The form is wrapped in a {@code VerticalLayout} to drop its default 100% width, so
     * the stylesheet alone describes the column's width. The preview is wrapped in a
     * {@code VerticalLayout} to add padding and stretch it to fill the column.
     */
    private void buildLayout() {
        // Pinned to the visible tab panel, like the product form is on its own: the
        // panel is a slotted child, so a percentage height here would resolve against
        // the whole tab sheet and push the row's bottom, and the form's action bar
        // with it, below the fold.
        addClassName("product-editor");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);
        mainLayout.setPadding(false);
        mainLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        mainLayout.addClassName("product-editor-layout");

        VerticalLayout formWrapper = new VerticalLayout(productForm);
        // No padding and no card chrome: the form brings its own surface, and
        // wrapping it in a second bordered box would draw a card inside a card.
        formWrapper.setPadding(false);
        formWrapper.addClassName("product-form-wrapper");
        releaseDefaultWidth(formWrapper);

        VerticalLayout previewWrapper = new VerticalLayout(productPreview);
        previewWrapper.setPadding(true);
        previewWrapper.setAlignItems(FlexComponent.Alignment.STRETCH);
        previewWrapper.addClassName("product-preview-wrapper");
        releaseDefaultWidth(previewWrapper);

        mainLayout.add(formWrapper, previewWrapper);

        add(mainLayout);
        setFlexGrow(1, mainLayout);
    }

    /**
     * Drops the inline {@code width: 100%} that {@code VerticalLayout}'s constructor
     * puts on every instance, so the stylesheet alone describes the column's width.
     *
     * @param column the wrapper whose constructor default should not apply
     */
    private static void releaseDefaultWidth(VerticalLayout column) {
        column.getElement().getStyle().remove("width");
    }
}
