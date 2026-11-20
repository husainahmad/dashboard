package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import com.harmoni.menu.dashboard.util.Messages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CategoryDetailCardTest {

    @Test
    void viewerSeesNoEditOrDeleteActions() {
        CategoryDetailCard card = new CategoryDetailCard();
        card.setCanEdit(false);
        card.setCanDelete(false);
        card.show(new CategoryDto(), "Brand");

        assertNull(UiTestSupport.buttonByText(card, Messages.get(Messages.Keys.ACTION_EDIT)));
        assertNull(UiTestSupport.buttonByText(card, Messages.get(Messages.Keys.ACTION_DELETE)));
    }

    @Test
    void editorSeesOnlyThePermittedActions() {
        CategoryDetailCard card = new CategoryDetailCard();
        card.setCanEdit(true);
        card.setCanDelete(false);
        card.show(new CategoryDto(), "Brand");

        assertNotNull(UiTestSupport.buttonByText(card, Messages.get(Messages.Keys.ACTION_EDIT)));
        assertNull(UiTestSupport.buttonByText(card, Messages.get(Messages.Keys.ACTION_DELETE)));
    }
}
