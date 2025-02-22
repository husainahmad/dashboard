package com.harmoni.menu.dashboard.layout.menu;

import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

@Getter
public class ProductFormLayout extends FormLayout  {

    private UI ui;

    public ProductFormLayout() {
        this.getElement().addEventListener("keydown", domEvent -> {}).stopPropagation();
    }

    public HorizontalLayout getContent(Component component) {
        HorizontalLayout content = new HorizontalLayout(component);
        content.setFlexGrow(1, component);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    public HorizontalLayout getToolbar(Component component) {
        HorizontalLayout toolbar = new HorizontalLayout(component);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    public void showNotification(String text) {
        ui.access(()->{
            Notification notification = new Notification(text, 3000,
                    Notification.Position.MIDDLE);
            notification.open();
        });
    }

    public void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
    }
}
