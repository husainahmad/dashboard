package com.harmoni.menu.dashboard.configuration;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.ImageDto;
import com.harmoni.menu.dashboard.dto.JwtDto;
import com.harmoni.menu.dashboard.dto.LoginDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.ProductImageDto;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.SubServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierMenuDto;
import com.harmoni.menu.dashboard.dto.TierServiceDto;
import com.harmoni.menu.dashboard.dto.TierSubServiceDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.enums.ProductItemAction;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import com.harmoni.menu.dashboard.layout.enums.RoleType;
import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import com.harmoni.menu.dashboard.layout.menu.product.ProductTreeItem;
import com.harmoni.menu.dashboard.layout.menu.product.SkuTreeItem;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.menu.TierMenuTreeItem;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TierServiceTreeItem;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.layout.setting.service.ServiceTreeItem;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.List;

@Configuration
@ImportRuntimeHints(NativeImageRuntimeHints.NativeRuntimeHintsRegistrar.class)
public class NativeImageRuntimeHints {

    static class NativeRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

        private static final List<Class<?>> CLASSES = List.of(
                BroadcastMessage.class,
                RestAPIResponse.class,
                AuthProperties.class,
                AuthUrlProperties.class,
                CategoryProperties.class,
                ImageProperties.class,
                MenuProperties.class,
                ProductProperties.class,
                SettingProperties.class,
                SettingUrlProperties.class,
                TierMenuProperties.class,
                TierProperties.class,
                TierServiceProperties.class,
                UrlProperties.class,
                UserProperties.class,
                StoreDto.class,
                TierDto.class,
                ProductImageDto.class,
                SubServiceDto.class,
                TierSubServiceDto.class,
                UserDto.class,
                SkuTierPriceDto.class,
                ChainDto.class,
                ServiceDto.class,
                BrandDto.class,
                LoginDto.class,
                SkuDto.class,
                TierServiceDto.class,
                JwtDto.class,
                TierMenuDto.class,
                CustomizationDto.class,
                ImageDto.class,
                CustomizationOptionDto.class,
                ProductDto.class,
                CategoryDto.class,
                TierTypeDto.class,
                ProductItemType.class,
                ProductItemAction.class,
                RoleType.class,
                SelectionType.class,
                FormAction.class,
                TreeLevel.class,
                ServiceTreeItem.class,
                ProductTreeItem.class,
                SkuTreeItem.class,
                TierServiceTreeItem.class,
                TierMenuTreeItem.class);

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            List<MemberCategory> memberCategories = List.of(
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.PUBLIC_FIELDS,
                    MemberCategory.DECLARED_FIELDS);
            CLASSES.forEach(clazz -> {
                hints.reflection().registerType(clazz, memberCategories.toArray(new MemberCategory[0]));
                hints.serialization().registerType(TypeReference.of(clazz));
            });
        }
    }
}
