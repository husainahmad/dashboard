package com.harmoni.menu.dashboard.layout.menu.product;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the two-column wiring of the product editor.
 *
 * <p>Only the structure is checked here. Whether the two columns actually sit side by
 * side, and whether the form stops painting over the preview, is decided by the
 * stylesheet and can only be confirmed in a browser - the bug this file exists to
 * prevent was purely a CSS one, and no assertion in this suite could have caught it.</p>
 */
class ProductFormWithPreviewTest {

    @Test
    void itHostsTheFormAndThePreviewTogether() {
        ProductFormWithPreview editor = newEditor();

        assertNotNull(editor.getProductForm());
        assertNotNull(editor.getProductPreview());
        assertTrue(contains(editor, editor.getProductPreview()),
                "a preview built but never added would leave the operator with a form and "
                        + "no way to see the result");
    }

    @Test
    void thePreviewIsAttachedToTheForm() {
        ProductFormWithPreview editor = newEditor();

        editor.getProductForm().firePreviewUpdate();

        // Detaching is silent by design, so the observable consequence is that the
        // preview has taken the form's state: the placeholder name for a product that
        // has not been named yet.
        assertTrue(text(editor.getProductPreview()).contains("preview.product.unnamed"),
                "the form has to know the preview, or nothing repaints it as the form changes");
    }

    @Test
    void itsRootCarriesTheClassThePanelPinningDependsOn() {
        ProductFormWithPreview editor = newEditor();

        assertTrue(editor.getClassNames().contains("product-editor"),
                "the root is pinned to the visible panel through this class; without it the "
                        + "row resolves its height against the whole tab sheet and the form's "
                        + "action bar falls below the fold");
    }

    // ------------------------------------------------------------- tab close

    @Test
    void aSavedProductClosesItsTabEvenThoughTheFormIsWrapped() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        tabSheet.add(browseTab, new Div());
        Tab editorTab = new Tab();
        ProductFormWithPreview editor = new ProductFormWithPreview(null, null,
                ProductEditorContext.builder()
                        .productTab(editorTab)
                        .tabManager(new TabManager(tabSheet))
                        .build());
        tabSheet.add(editorTab, editor);

        // closeEditorTab, not removeFromSheet: the latter routes through UiUtil.safeAccess,
        // which returns immediately without a session and so would assert nothing.
        editor.getProductForm().closeEditorTab();

        assertEquals(1, tabSheet.getTabCount(),
                "a product saved with a 201 has to close its own tab. A tab sheet does not "
                        + "make its content a child of itself, so the form has to be handed "
                        + "the manager directly or the close finds nothing to do");
    }



    @Test
    void anExistingProductOffersUpdateRatherThanSave() {
        ProductTreeItem existing = ProductTreeItem.builder().productId(7).name("Latte").build();
        TabSheet tabSheet = new TabSheet();
        Tab editorTab = new Tab();
        ProductFormWithPreview editor = new ProductFormWithPreview(null, null,
                ProductEditorContext.builder()
                        .productTab(editorTab)
                        .productTreeItem(existing)
                        .tabManager(new TabManager(tabSheet))
                        .build());

        rendered(editor.getProductForm());

        assertFalse(editor.getProductForm().saveButton.isVisible(),
                "create answers 201 and update answers 200, and each listener treats the "
                        + "other status as a failure, so offering Save on an existing "
                        + "product can only fail quietly");
        assertTrue(editor.getProductForm().updateButton.isVisible(),
                "an existing product is updated, not created");
    }

    @Test
    void aNewProductOffersSaveRatherThanUpdate() {
        TabSheet tabSheet = new TabSheet();
        Tab editorTab = new Tab();
        ProductFormWithPreview editor = new ProductFormWithPreview(null, null,
                ProductEditorContext.builder()
                        .productTab(editorTab)
                        .tabManager(new TabManager(tabSheet))
                        .build());

        rendered(editor.getProductForm());

        assertTrue(editor.getProductForm().saveButton.isVisible());
        assertFalse(editor.getProductForm().updateButton.isVisible(),
                "updating a product that was never saved has no id to update");
    }

    /**
     * Runs the attach-time setup, which decides the action buttons. There is no session
     * here so {@code onAttach} never fires, the same limitation
     * {@code ProductImageUploadViewTest} works around.
     */
    private static void rendered(ProductForm form) {
        try {
            java.lang.reflect.Method renderLayout =
                    ProductForm.class.getDeclaredMethod("renderLayout");
            renderLayout.setAccessible(true);
            renderLayout.invoke(form);
        } catch (InvocationTargetException e) {
            throw new AssertionError("renderLayout failed", e.getCause());
        } catch (IllegalAccessException e) {
            throw new AssertionError("renderLayout is not accessible", e);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("renderLayout is missing", e);
        }
    }

    private static ProductFormWithPreview newEditor() {
        return new ProductFormWithPreview(null, null, ProductEditorContext.builder().build());
    }

    private static boolean contains(Component root, Component wanted) {
        if (root == wanted) {
            return true;
        }
        return root.getChildren().anyMatch(child -> contains(child, wanted));
    }

    private static String text(Component root) {
        StringBuilder text = new StringBuilder(root.getElement().getText());
        for (Component child : root.getChildren().toList()) {
            text.append(' ').append(text(child));
        }
        return text.toString();
    }
}
