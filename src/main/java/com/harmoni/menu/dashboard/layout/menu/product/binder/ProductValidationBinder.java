package com.harmoni.menu.dashboard.layout.menu.product.binder;

import com.harmoni.menu.dashboard.layout.menu.product.ProductDialogEdit;
import com.vaadin.flow.data.binder.BeanValidationBinder;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProductValidationBinder extends BeanValidationBinder<ProductBinderBean> {

    public ProductValidationBinder(Class<ProductBinderBean> beanType,
                                   ProductDialogEdit productDialogEdit, Integer skuId) {
        super(beanType);

        AtomicBoolean isAlready = new AtomicBoolean(false);

        productDialogEdit.getBinders().forEach(productBinderBeanBinder -> {
            if (productBinderBeanBinder.getBean().getSkuId().equals(skuId)) {
                this.setBean(productBinderBeanBinder.getBean());
                isAlready.set(true);
            }
        });

        if (!isAlready.get()) {
            productDialogEdit.getBinders().add(this);
        }

    }
}
