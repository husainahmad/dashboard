package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * The title and explanatory caption a page opens with.
 *
 * <p>One block for every page that introduces itself, so the report pages and the
 * list pages cannot drift apart: same heading level, same caption styling, same
 * spacing. Pages supply the two strings and get the layout for free.</p>
 *
 * <p>The caption is optional. A page with nothing useful to say about itself passes
 * null and gets the heading alone, rather than an empty paragraph holding the space
 * a caption would have filled.</p>
 */
public class PageHeader extends VerticalLayout {

    /**
     * @param title       the page heading, required
     * @param description the explanatory caption, may be null
     */
    public PageHeader(String title, String description) {
        H2 heading = new H2(title);
        add(heading);
        if (description != null && !description.isBlank()) {
            Paragraph caption = new Paragraph(description);
            caption.addClassName(Css.HEALTH_CAPTION);
            add(caption);
        }
        setPadding(false);
        setAlignItems(FlexComponent.Alignment.START);
        setWidthFull();
    }

    /**
     * Builds a header from message keys, so the text stays translatable.
     *
     * @param titleKey       message key for the heading
     * @param descriptionKey message key for the caption, may be null
     * @return the header
     */
    public static PageHeader of(String titleKey, String descriptionKey) {
        return new PageHeader(Messages.get(titleKey),
                descriptionKey == null ? null : Messages.get(descriptionKey));
    }
}
