package com.harmoni.menu.dashboard.layout;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | POSHarmoni")
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        addClassName("dashboard-view");
        setSizeFull();

        add(createHero());
        add(createStatCards());
        add(createPanels());
    }

    private VerticalLayout createHero() {
        H2 title = new H2("Welcome back");
        Paragraph subtitle = new Paragraph("Manage your menu, organization and store settings from one place.");

        VerticalLayout hero = new VerticalLayout(title, subtitle);
        hero.addClassName("dashboard-hero");
        hero.setWidthFull();
        hero.setAlignItems(FlexComponent.Alignment.START);
        return hero;
    }

    private HorizontalLayout createStatCards() {
        StatCard brands = new StatCard(VaadinIcon.SHOP, "Business units", "Brands, chains & tiers");
        StatCard products = new StatCard(VaadinIcon.COFFEE, "Menu items", "Products, SKUs & customizations");
        StatCard users = new StatCard(VaadinIcon.USER, "Team", "Users & store access");
        StatCard services = new StatCard(VaadinIcon.COG, "Services", "Setup & rates");

        HorizontalLayout cards = new HorizontalLayout(brands, products, users, services);
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.setPadding(false);
        cards.setFlexGrow(1, brands, products, users, services);
        cards.setAlignItems(FlexComponent.Alignment.STRETCH);
        return cards;
    }

    private VerticalLayout createPanels() {
        H2 storesTitle = new H2("Quick guidance");
        Paragraph p1 = new Paragraph("Start by configuring your Brand and Chain under Admin, then build your " +
                "menu with Categories, Customizations and Products.");
        Paragraph p2 = new Paragraph("Use the Tier section to define pricing and available services across your stores.");

        VerticalLayout panel = new VerticalLayout(storesTitle, p1, p2);
        panel.addClassName("panel");
        panel.setWidthFull();
        panel.setAlignItems(FlexComponent.Alignment.START);
        return panel;
    }

    private static class StatCard extends HorizontalLayout {

        StatCard(VaadinIcon icon, String value, String label) {
            Icon iconComponent = icon.create();
            iconComponent.getStyle().set("--vaadin-icon-size", "24px");
            iconComponent.addClassName("stat-icon");

            H2 valueLabel = new H2(value);
            valueLabel.addClassName("stat-value");
            Paragraph caption = new Paragraph(label);
            caption.addClassName("stat-label");

            VerticalLayout texts = new VerticalLayout(valueLabel, caption);
            texts.setPadding(false);
            texts.setSpacing(false);

            add(iconComponent, texts);
            setAlignItems(FlexComponent.Alignment.CENTER);
            setSpacing(true);
            addClassName("stat-card");
            setWidth("25%");
            setFlexGrow(1);
        }
    }
}