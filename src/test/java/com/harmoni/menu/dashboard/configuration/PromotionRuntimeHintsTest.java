package com.harmoni.menu.dashboard.configuration;

import com.harmoni.menu.dashboard.configuration.NativeImageRuntimeHints.NativeRuntimeHintsRegistrar;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionRuleDto;
import com.harmoni.menu.dashboard.dto.PromotionScheduleDto;
import com.harmoni.menu.dashboard.dto.PromotionSpecialPriceDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionRuleType;
import com.harmoni.menu.dashboard.layout.enums.PromotionScopeType;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the promotion module's native-image registration. Every one of these types
 * is deserialised reflectively by Jackson, so an unregistered class only fails under
 * {@code -Pnative native:compile}, long after the change that broke it.
 */
class PromotionRuntimeHintsTest {

    private static final List<Class<?>> PROMOTION_WIRE_TYPES = List.of(
            PromotionDto.class,
            PromotionScheduleDto.class,
            PromotionTargetDto.class,
            PromotionRuleDto.class,
            PromotionSpecialPriceDto.class,
            PromotionType.class,
            PromotionStatus.class,
            PromotionTargetType.class,
            PromotionScopeType.class,
            PromotionRuleType.class);

    @Test
    void everyPromotionWireType_isRegisteredForNativeImage() throws Exception {
        List<Class<?>> registered = registeredClasses();

        for (Class<?> type : PROMOTION_WIRE_TYPES) {
            assertTrue(registered.contains(type),
                    type.getSimpleName() + " crosses the promotion REST boundary and must be listed in "
                            + "NativeImageRuntimeHints.CLASSES, otherwise a native build fails on Jackson reflection");
        }
    }

    @Test
    void promotionProperties_areRegisteredForNativeImage() throws Exception {
        assertTrue(registeredClasses().contains(PromotionProperties.class),
                "PromotionProperties binds menu.url.promotions.* and must be registered like every other "
                        + "@ConfigurationProperties class");
    }

    @SuppressWarnings("unchecked")
    private static List<Class<?>> registeredClasses() throws Exception {
        Field field = NativeRuntimeHintsRegistrar.class.getDeclaredField("CLASSES");
        field.setAccessible(true);
        return (List<Class<?>>) field.get(null);
    }
}
