package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
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

    private boolean rendered;

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
        AbstractListView listView = createListView();

        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get(tabLabelKey()));
        tabSheet.add(browseTab, listView);
        tabSheet.setSizeFull();

        add(PageHeader.of(pageTitleKey(), pageDescriptionKey()));
        add(listView.getToolbarComponent());
        add(tabSheet);
        setFlexGrow(1, tabSheet);
        setPadding(false);
        setSizeFull();
    }
}
