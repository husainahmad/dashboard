package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.vaadin.flow.component.select.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards how the store picker's fields label their options.
 *
 * <p>Left to itself a Select falls back to the option's {@code toString()}, and these
 * DTOs record every column they carry. The operator was therefore choosing between lines
 * like {@code BrandDto(id=1, name=Kopi Harmoni, brandId=null, ...)} rather than between
 * brand names - which is what the same fields show in the form itself.</p>
 *
 * <p>Covers the two Selects. The store multi-select takes its generator through the same
 * {@code configure} call but does not expose it for reading, so there is nothing to
 * assert on it here.</p>
 */
class StoreSelectionDialogOptionLabelTest {

    @Test
    void theBrandField_showsTheBrandNameAndNotTheWholeRecord() throws Exception {
        Select<BrandDto> field = field("brandSelect");

        assertEquals("Kopi Harmoni", field.getItemLabelGenerator().apply(brand()),
                "the operator picks between brand names, not between field assignments");
    }

    @Test
    void theChainField_showsTheChainNameAndNotTheWholeRecord() throws Exception {
        Select<ChainDto> field = field("chainSelect");

        assertEquals("Harmoni Coffee", field.getItemLabelGenerator().apply(chain()),
                "the chain's name is what identifies it to the operator");
    }

    @SuppressWarnings("unchecked")
    private static <T> Select<T> field(String name) throws Exception {
        Field field = StoreSelectionDialog.class.getDeclaredField(name);
        field.setAccessible(true);
        StoreSelectionDialog dialog = new StoreSelectionDialog(
                (AsyncRestClientOrganizationService) null, null, null, List.of(), selection -> {
        });
        return (Select<T>) field.get(dialog);
    }

    private static BrandDto brand() {
        BrandDto brand = new BrandDto();
        brand.setId(1);
        brand.setName("Kopi Harmoni");
        return brand;
    }

    private static ChainDto chain() {
        ChainDto chain = new ChainDto();
        chain.setId(1);
        chain.setName("Harmoni Coffee");
        return chain;
    }
}
