package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
 *
 * <p>It carries {@link Css#PAGE_HEADER} so a page that can collapse its own chrome has
 * something to act on: the header is one of the two elements focus mode takes out of
 * the way, and until now nothing named it.</p>
 */
public class PageHeader extends VerticalLayout {

    /**
     * @param title       the page heading, required
     * @param description the explanatory caption, may be null
     */
    public PageHeader(String title, String description) {
        this(title, description, null);
    }

    /**
     * @param title       the page heading, required
     * @param description the explanatory caption, may be null
     * @param action      an optional action shown at the trailing edge, may be null
     */
    public PageHeader(String title, String description, Component action) {
        VerticalLayout text = new VerticalLayout();
        text.setPadding(false);
        text.setSpacing(false);
        text.setWidthFull();
        text.add(new H2(title));
        if (description != null && !description.isBlank()) {
            Paragraph caption = new Paragraph(description);
            caption.addClassName(Css.HEALTH_CAPTION);
            text.add(caption);
        }

        if (action == null) {
            add(text);
        } else {
            HorizontalLayout row = new HorizontalLayout(text, action);
            row.setWidthFull();
            row.setAlignItems(FlexComponent.Alignment.START);
            row.setFlexGrow(1, text);
            add(row);
        }

        addClassName(Css.PAGE_HEADER);
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

    /**
     * Builds a header with a trailing action from message keys.
     *
     * @param titleKey       message key for the heading
     * @param descriptionKey message key for the caption, may be null
     * @param action         the action shown at the trailing edge
     * @return the header
     */
    public static PageHeader of(String titleKey, String descriptionKey, Component action) {
        return new PageHeader(Messages.get(titleKey),
                descriptionKey == null ? null : Messages.get(descriptionKey), action);
    }
}
