package com.harmoni.menu.dashboard.layout.menu.product;


import com.vaadin.flow.component.button.Button;
import lombok.Getter;

public class ProductEditButton extends Button {

    @Getter
    private ProductTreeItem productTreeItem;

    public ProductEditButton(ProductTreeItem productTreeItem) {
        this.productTreeItem = productTreeItem;
        setText("Edit");
    }
}
