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
 * <p>Mirrors {@code PromotionFormWithPreview} so the two editors read the same way, and
 * borrows its column sizing: both columns are described by stylesheet class names rather
 * than widths set in Java. An inline {@code width} outranks every rule in the stylesheet
 * however specific, which is what used to make the single-column fallback unreachable on
 * a narrow screen.</p>
 *
 * <p>Not a route. The editor is only ever built by {@link ProductListView} inside its
 * parent {@code TabSheet}, and it needs the {@code Tab} it was opened on, which is not a
 * Spring bean, so it is constructed directly rather than autowired.</p>
 *
 * <p>The action buttons stay inside {@link ProductForm} rather than moving down here as
 * they do for promotions: the product form already lays out its own footer, and lifting
 * it out would have meant reimplementing the binder-driven Save/Update visibility that
 * form already owns.</p>
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
