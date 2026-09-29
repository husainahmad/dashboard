package com.harmoni.menu.dashboard.layout.menu.product;

import com.vaadin.flow.dom.Style;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards that the product form carries no inline width or height.
 *
 * <p>{@code setSizeFull()} on the form wrote {@code width: 100%; height: 100%} onto the
 * element, and an inline value outranks every rule in the stylesheet however specific. Every
 * attempt to describe the form's height in forms.css was therefore dead in the editor, which
 * is why a long form's action bar could not be reached: the inline height pinned the form to
 * the column and left the content with nowhere to go.</p>
 */
class ProductFormInlineSizeTest {

    @Test
    void theForm_declaresNoInlineWidthOrHeight() throws Exception {
        var form = new ProductForm(null, null,
                ProductEditorContext.builder().categoryDtos(java.util.List.of())
                        .tierDtos(java.util.List.of()).build());
        Method render = ProductForm.class.getDeclaredMethod("renderLayout");
        render.setAccessible(true);
        render.invoke(form);

        Style style = form.getElement().getStyle();
        for (String property : new String[]{"width", "height", "min-height", "max-height", "flex"}) {
            String declared = style.get(property);
            assertTrue(declared == null || declared.isEmpty(),
                    "the form sets an inline '" + property + "' (" + declared + "), which outranks forms.css "
                            + "however specific the rule is - its size belongs in the stylesheet, where the "
                            + "editor and standalone rules can each describe it");
        }
    }
}
