package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.event.product.ProductDeleteEventListener;
import com.harmoni.menu.dashboard.layout.menu.product.binder.ProductBinderBean;
import com.harmoni.menu.dashboard.layout.menu.product.ProductDialogEdit;
import com.harmoni.menu.dashboard.layout.menu.product.binder.ProductValidationBinder;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.util.SkuUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.util.ObjectUtils;

import java.util.List;

public class SkuComponentRender extends HorizontalLayout {

    private final transient SkuDto skuDto;
    private final ProductDialogEdit productDialogEdit;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;

    public SkuComponentRender(SkuDto skuDto, ProductDialogEdit productDialogEdit,
                              AsyncRestClientMenuService asyncRestClientMenuService, RestClientMenuService restClientMenuService) {
        this.skuDto = skuDto;
        this.productDialogEdit = productDialogEdit;
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.restClientMenuService = restClientMenuService;
        this.render();
    }

    public void render() {
        final TextField nameField = new TextField();
        nameField.setValue(ObjectUtils.isEmpty(skuDto.getName()) ? "" : skuDto.getName());

        final VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(false);

        add(nameField, verticalLayout);
        setPadding(false);
        setFlexGrow(2);
        fetchPriceBySku(ObjectUtils.isEmpty(skuDto.getId()) ? -1 : skuDto.getId(), verticalLayout, nameField);
    }

    private void fetchPriceBySku(Integer skuId, VerticalLayout verticalLayout, TextField skuNameField) {

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        final BeanValidationBinder<ProductBinderBean> binder = getBinder(skuId);

        binder.forField(skuNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(ProductBinderBean::getSkuName, ProductBinderBean::setSkuName);

        final ProductBinderBean productBinderBean = !ObjectUtils.isEmpty(binder.getBean()) ? binder.getBean()
                : ProductBinderBean.builder()
                .skuId(skuId)
                .skuName(skuNameField.getValue())
                .tierId(productDialogEdit.getTierDto().getId())
                .price(0.0)
                .build();

        final TextField text = new TextField();
        text.setValue(this.productDialogEdit.getTierDto().getName());
        text.setEnabled(false);

        final NumberField priceField = new NumberField();

        binder.forField(priceField)
                .bind(ProductBinderBean::getPrice, ProductBinderBean::setPrice);

        binder.setBean(productBinderBean);

        Button buttonDelete = getButtonDelete(binder);

        horizontalLayout.add(text, priceField, buttonDelete);

        this.productDialogEdit.getUi().access(() -> verticalLayout.add(horizontalLayout));

        if (skuId.equals(productDialogEdit.getProductTreeItem()
                        .getSkus().getLast().getId())) {
            fetchPrice(productDialogEdit.getProductTreeItem().getSkus());
        }
    }

    private BeanValidationBinder<ProductBinderBean> getBinder(Integer skuId) {
        return new ProductValidationBinder(ProductBinderBean.class,
                this.productDialogEdit, skuId);
    }

    private void fetchPrice(List<SkuDto> skus) {
        asyncRestClientMenuService.getDetailSkuTierPriceAsync(result -> {
            for (SkuTierPriceDto skuTierPriceDto : result) {
                setPriceFromServer(skuTierPriceDto, this.productDialogEdit.getBinders(),
                        this.productDialogEdit.getUi());
            }
        }, SkuUtil.getIdsByList(skus), this.productDialogEdit.getTierDto().getId());
    }


    private Button getButtonDelete(BeanValidationBinder<ProductBinderBean> binder) {
        return new Button(new Icon(VaadinIcon.MINUS),
                new ProductDeleteEventListener(
                        this.restClientMenuService, null));
    }

    private void setPriceFromServer(SkuTierPriceDto skuTierPriceDto, List<Binder<ProductBinderBean>> currentBinder, UI currentUi) {
        for (Binder<ProductBinderBean> productBinder : currentBinder) {
            if (productBinder.getBean().getSkuId().equals(skuTierPriceDto.getSkuId())) {
                productBinder.getFields().forEach(hasValue -> {
                    if (hasValue instanceof NumberField numberField) {
                        currentUi.access(() -> numberField.setValue(skuTierPriceDto.getPrice()));
                    }
                });
            }
        }
    }


}
