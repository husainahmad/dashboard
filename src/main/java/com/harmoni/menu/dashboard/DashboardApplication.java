package com.harmoni.menu.dashboard;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Theme(variant = Lumo.DARK)
@Push(transport = Transport.LONG_POLLING)
@SpringBootApplication
public class DashboardApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(DashboardApplication.class, args);
	}

}
