package com.benguides.frontend.views.superadmin;

import com.benguides.frontend.layout.MainLayout;
import com.benguides.models.Company;
import com.benguides.services.CompanyService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;

@Route(value = "super-admin/dashboard", layout = MainLayout.class)
@RolesAllowed("ROLE_SUPER_ADMIN")
public class SuperAdminView extends VerticalLayout {

    private final CompanyService companyService;
    private Grid<Company> companyGrid;

    @Autowired
    public SuperAdminView(CompanyService companyService) {
        this.companyService = companyService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createCompanyGrid();
        loadCompanies();
    }

    private void createHeader() {
        H1 header = new H1("Super Administrator Dashboard");
        header.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("margin-bottom", "2rem");
        add(header);
    }

    private void createCompanyGrid() {
        companyGrid = new Grid<>(Company.class, false);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        companyGrid.addColumn(Company::getName)
                .setHeader("Company Name")
                .setAutoWidth(true);

        companyGrid.addColumn(Company::getEmail)
                .setHeader("Email")
                .setAutoWidth(true);

        companyGrid.addColumn(Company::getPhone)
                .setHeader("Phone")
                .setAutoWidth(true);

        companyGrid.addColumn(Company::getSubscriptionPlan)
                .setHeader("Plan")
                .setAutoWidth(true);

        companyGrid.addColumn(company -> company.isActive() ? "Active" : "Inactive")
                .setHeader("Status")
                .setAutoWidth(true);
        companyGrid.addColumn(company -> company.getCreatedAt() != null
                        ? company.getCreatedAt().format(formatter)
                        : "—")
                .setHeader("Created At")
                .setAutoWidth(true);

        companyGrid.addColumn(company -> company.getUpdatedAt() != null
                        ? company.getUpdatedAt().format(formatter)
                        : "—")
                .setHeader("Last Updated")
                .setAutoWidth(true);

        companyGrid.setHeight("500px");
        add(companyGrid);
    }

    private void loadCompanies() {
        companyGrid.setItems(companyService.getAllCompanies());
    }
}

/*
package com.benguides.frontend.views.superadmin;

import com.benguides.frontend.layout.MainLayout;
import com.benguides.models.Company;
import com.benguides.services.CompanyService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "super-admin/dashboard", layout = MainLayout.class)
@RolesAllowed("ROLE_SUPER_ADMIN")
public class SuperAdminView extends VerticalLayout {

    private final CompanyService companyService;
    private Grid<Company> companyGrid;

    @Autowired
    public SuperAdminView(CompanyService companyService) {
        this.companyService = companyService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createCompanyGrid();
        loadCompanies();
    }

    private void createHeader() {
        H1 header = new H1("Super Administrator Dashboard");
        header.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("margin-bottom", "2rem");
        add(header);
    }

    private void createCompanyGrid() {
        companyGrid = new Grid<>(Company.class, false);
        companyGrid.addColumn(Company::getName).setHeader("Company Name").setAutoWidth(true);
        companyGrid.addColumn(Company::getEmail).setHeader("Email").setAutoWidth(true);
        companyGrid.addColumn(Company::getPhone).setHeader("Phone").setAutoWidth(true);
        companyGrid.addColumn(Company::getSubscriptionPlan).setHeader("Plan").setAutoWidth(true);
        companyGrid.addColumn(company -> company.isActive() ? "Active" : "Inactive")
                .setHeader("Status").setAutoWidth(true);

        companyGrid.setHeight("400px");
        add(companyGrid);
    }

    private void loadCompanies() {
        companyGrid.setItems(companyService.getAllCompanies());
    }
}
*/
