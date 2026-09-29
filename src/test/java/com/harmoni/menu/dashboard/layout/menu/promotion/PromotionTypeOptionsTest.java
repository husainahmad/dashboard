package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.vaadin.flow.component.select.Select;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the withdrawal of {@code SPECIAL_PRICE} from the type list.
 *
 * <p>It is withheld because its rows are not validated and switching away from it orphans
 * them on the record. It is still reinstated for a promotion that already uses one, so
 * hiding it cannot quietly change the type of saved data.</p>
 */
class PromotionTypeOptionsTest {

    @Test
    void specialPrice_isNotOfferedForANewPromotion() throws Exception {
        Select<PromotionType> type = typeSelectOf(newForm(FormAction.CREATE, null));

        assertTrue(type.getListDataView().getItems().noneMatch(PromotionType.SPECIAL_PRICE::equals),
                "SPECIAL_PRICE is withheld until its rows are validated - found " + items(type));
    }

    @Test
    void theOtherTypesAreStillOffered() throws Exception {
        Select<PromotionType> type = typeSelectOf(newForm(FormAction.CREATE, null));

        assertTrue(type.getListDataView().getItems().anyMatch(PromotionType.PERCENTAGE::equals),
                "percentage has to stay selectable - found " + items(type));
        assertTrue(type.getListDataView().getItems().anyMatch(PromotionType.FIXED_AMOUNT::equals),
                "fixed amount has to stay selectable - found " + items(type));
    }

    @Test
    void aNewPromotion_stillDefaultsToAPercentage() throws Exception {
        // The default must not be a withdrawn type, or a new form would open on a value the
        // select cannot show.
        assertEquals(PromotionType.PERCENTAGE, typeSelectOf(newForm(FormAction.CREATE, null)).getValue());
    }

    @Test
    void aSavedSpecialPricePromotion_stillRestoresItsType() throws Exception {
        PromotionDto saved = new PromotionDto();
        saved.setPromotionType(PromotionType.SPECIAL_PRICE);

        Select<PromotionType> type = typeSelectOf(newForm(FormAction.EDIT, saved));

        assertTrue(type.getListDataView().getItems().anyMatch(PromotionType.SPECIAL_PRICE::equals),
                "the option has to be put back for a promotion that already uses it, or the binder has nothing "
                        + "to restore the type into and saving would change it");
        assertEquals(PromotionType.SPECIAL_PRICE, type.getValue(),
                "a saved special price promotion must round-trip its type, or saving it would rewrite the type "
                        + "and leave the price rows orphaned on the record");
    }

    @Test
    void aSavedPromotionOfAnotherType_doesNotRegainTheOption() throws Exception {
        PromotionDto saved = new PromotionDto();
        saved.setPromotionType(PromotionType.PERCENTAGE);

        Select<PromotionType> type = typeSelectOf(newForm(FormAction.EDIT, saved));

        assertTrue(type.getListDataView().getItems().noneMatch(PromotionType.SPECIAL_PRICE::equals),
                "reinstating the option is for the promotion that needs it, not for every saved promotion - found "
                        + items(type));
    }

    private static String items(Select<PromotionType> type) {
        return type.getListDataView().getItems().toList().toString();
    }

    @SuppressWarnings("unchecked")
    private static Select<PromotionType> typeSelectOf(PromotionForm form) throws Exception {
        Field declared = PromotionForm.class.getDeclaredField("typeSelect");
        declared.setAccessible(true);
        return (Select<PromotionType>) declared.get(form);
    }

    private static PromotionForm newForm(FormAction formAction, PromotionDto promotion) throws Exception {
        PromotionForm form = new PromotionForm(null, null, null, null,
                PromotionEditorContext.builder().formAction(formAction).promotionDto(promotion).build());
        invoke(form, "configureFields");
        invoke(form, "configureSpecialPriceGrid");
        invoke(form, "addValidation");
        invoke(form, "addFields");
        invoke(form, "restoreBean");
        return form;
    }

    private static void invoke(PromotionForm form, String name) throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name);
        method.setAccessible(true);
        assertNotNull(method);
        method.invoke(form);
    }
}
