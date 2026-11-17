package com.harmoni.menu.dashboard.layout.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Guards where a form tab sends the operator when it closes.
 *
 * <p>On the table page the first tab is the floor plan, not the list, so
 * "select the first tab" would drop the operator on the wrong view. Closing a
 * form has to return to the tab that was active when the form opened.</p>
 */
class TabManagerTest {

    @Test
    void closingAFormReturnsToTheTabItWasOpenedFrom() {
        TabSheet tabSheet = new TabSheet();
        Tab planTab = new Tab();
        Tab listTab = new Tab();
        tabSheet.add(planTab, new Div());
        tabSheet.add(listTab, new Div());
        tabSheet.setSelectedTab(listTab);

        TabManager manager = new TabManager(tabSheet);
        Tab formTab = manager.addOrSelect("Edit Table 1", tab -> new Div());
        assertEquals(formTab, tabSheet.getSelectedTab());

        manager.closeAndSelectFirst(formTab);

        assertEquals(listTab, tabSheet.getSelectedTab(),
                "cancel must land on the list the operator came from, not the first tab");
        assertEquals(2, tabSheet.getTabCount());
    }

    @Test
    void closingAFormWithoutAPreviousTabFallsBackToTheFirstTab() {
        TabSheet tabSheet = new TabSheet();
        TabManager manager = new TabManager(tabSheet);
        Tab formTab = manager.addOrSelect("Edit Table 1", tab -> new Div());

        manager.closeAndSelectFirst(formTab);

        assertEquals(0, tabSheet.getTabCount());
    }

    @Test
    void closingABackgroundTabKeepsTheCurrentSelection() {
        TabSheet tabSheet = new TabSheet();
        Tab listTab = new Tab();
        tabSheet.add(listTab, new Div());
        tabSheet.setSelectedTab(listTab);

        TabManager firstManager = new TabManager(tabSheet);
        Tab formA = firstManager.addOrSelect("a", "Form A", tab -> new Div());
        TabManager secondManager = new TabManager(tabSheet);
        Tab formB = secondManager.addOrSelect("b", "Form B", tab -> new Div());

        tabSheet.setSelectedTab(formA);
        secondManager.closeAndSelectFirst(formB);

        assertEquals(formA, tabSheet.getSelectedTab(),
                "closing a background tab must not move the selection");
    }

    @Test
    void tabsWithTheSameLabelButDifferentKeysStaySeparate() {
        TabSheet tabSheet = new TabSheet();
        TabManager manager = new TabManager(tabSheet);

        Tab first = manager.addOrSelect("product:1", "Coffee", tab -> new Div());
        Tab second = manager.addOrSelect("product:2", "Coffee", tab -> new Div());

        assertNotEquals(first, second);
        assertEquals(2, tabSheet.getTabCount());
    }

    @Test
    void theSameKeySelectsTheExistingTabEvenWhenTheLabelChanged() {
        TabSheet tabSheet = new TabSheet();
        TabManager manager = new TabManager(tabSheet);

        Tab first = manager.addOrSelect("product:1", "Coffee", tab -> new Div());
        Tab again = manager.addOrSelect("product:1", "Coffee (renamed)", tab -> new Div());

        assertSame(first, again);
        assertEquals(1, tabSheet.getTabCount());
    }
}
