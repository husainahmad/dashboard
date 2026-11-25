package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;

/**
 * Shared shell for the single-tab list pages: a {@link PageHeader}, the list's
 * toolbar and a {@link TabSheet} holding the list view.
 *
 * <p>Every browse page used to repeat the same {@code renderTabSheet} /
 * {@code onAttach} boilerplate. Subclasses now only describe their list view and
 * the three message keys; the layout, the one-time render guard and the attach
 * wiring live here.</p>
 */
public abstract class AbstractListViewTabs extends VerticalLayout {

    private AbstractListView listView;
    private boolean rendered;

    protected AbstractListView currentListView() {
        return listView;
    }

    /**
     * Message key for the browse tab caption.
     *
     * @return the tab label key
     */
    protected abstract String tabLabelKey();

    /**
     * Message key for the page title.
     *
     * @return the title key
     */
    protected abstract String pageTitleKey();

    /**
     * Message key for the page description, or {@code null} for none.
     *
     * @return the description key
     */
    protected abstract String pageDescriptionKey();

    /**
     * Builds the list view shown in the browse tab. Called once, on first attach.
     *
     * @return the list view
     */
    protected abstract AbstractListView createListView();

    /**
     * Optional decorative icon shown before the browse tab caption.
     *
     * @return the icon, or {@code null} for a text-only tab
     */
    protected VaadinIcon tabIcon() {
        return null;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (rendered) {
            return;
        }
        rendered = true;
        renderTabSheet();
    }

    private void renderTabSheet() {
        listView = createListView();

        TabSheet tabSheet = new TabSheet();
        tabSheet.add(buildBrowseTab(), listView);
        tabSheet.setSizeFull();

        add(PageHeader.of(pageTitleKey(), pageDescriptionKey()));
        add(listView.getToolbarComponent());
        add(tabSheet);
        setFlexGrow(1, tabSheet);
        setPadding(false);
        setSizeFull();
    }

    private Tab buildBrowseTab() {
        String label = Messages.get(tabLabelKey());
        VaadinIcon icon = tabIcon();
        if (icon == null) {
            return new Tab(label);
        }
        Icon leading = new Icon(icon);
        leading.setSize("16px");
        leading.addClassName("tab-icon");
        return new Tab(leading, new Span(label));
    }
}
