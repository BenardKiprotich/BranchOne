package com.benguides.frontend.views.companyadmin;

import com.benguides.dtos.CompanyDTO;
import com.benguides.frontend.layout.MainLayout;
import com.benguides.models.Branch;
import com.benguides.models.Company;
import com.benguides.models.User;
import com.benguides.security.SecurityService;
import com.benguides.services.BranchService;
import com.benguides.services.CompanyService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Route(value = "company-admin/dashboard", layout = MainLayout.class)
@RolesAllowed("ROLE_COMPANY_ADMIN")
public class CompanyAdminView extends VerticalLayout {

    private final SecurityService securityService;
    private final CompanyService companyService;
    private final BranchService branchService;

    private Company currentCompany;
    private CompanyDTO companyStats;
    private Grid<Branch> branchGrid;
    private Button addBranchButton;
    private HorizontalLayout statsLayout;

    @Autowired
    public CompanyAdminView(SecurityService securityService, CompanyService companyService,
                            BranchService branchService) {
        this.securityService = securityService;
        this.companyService = companyService;
        this.branchService = branchService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        loadCurrentCompany();
        createHeader();
        createStatsCards();
        createBranchSection();
    }

    private void loadCurrentCompany() {
        Optional<User> currentUser = securityService.getAuthenticatedUser();
        if (currentUser.isPresent() && currentUser.get().getCompany() != null) {
            Long companyId = currentUser.get().getCompany().getId();
            currentCompany = companyService.getCompanyById(companyId).orElse(null);

            if (currentCompany != null) {
                Optional<CompanyDTO> stats = branchService.getCompanyStats(companyId);
                stats.ifPresent(dto -> companyStats = dto);
            }
        }
    }

    private void createHeader() {
        Div headerContainer = new Div();

        if (currentCompany != null) {
            headerContainer.getStyle()
                    .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                    .set("border-radius", "0.75rem")
                    .set("padding", "2rem")
                    .set("margin-bottom", "1.5rem")
                    .set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)")
                    .set("position", "relative")
                    .set("overflow", "hidden")
                    .set("color", "white");

            Div overlay = new Div();
            overlay.getStyle()
                    .set("position", "absolute")
                    .set("top", "0")
                    .set("left", "0")
                    .set("right", "0")
                    .set("bottom", "0")
                    .set("background", "radial-gradient(circle at 30% 20%, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 70%)")
                    .set("pointer-events", "none");
            headerContainer.add(overlay);

            H1 header = new H1(currentCompany.getName());
            header.getStyle()
                    .set("color", "white")
                    .set("font-weight", "800")
                    .set("font-size", "2.5rem")
                    .set("margin", "0")
                    .set("text-shadow", "0 2px 4px rgba(0,0,0,0.3)")
                    .set("position", "relative")
                    .set("z-index", "1");

            VerticalLayout headerContent = new VerticalLayout(header);
            headerContent.setSpacing(false);
            headerContent.setPadding(false);
            headerContent.setAlignItems(FlexComponent.Alignment.CENTER);
            headerContent.getStyle()
                    .set("position", "relative")
                    .set("z-index", "1");

            headerContainer.add(headerContent);
        } else {
            headerContainer.getStyle()
                    .set("background", "linear-gradient(135deg, #FFEDD8, #F7F9FA)")
                    .set("border-radius", "0.75rem")
                    .set("padding", "2rem")
                    .set("margin-bottom", "1.5rem")
                    .set("border", "2px dashed #FF7F11");

            H1 header = new H1("Company Admin Dashboard");
            header.getStyle()
                    .set("color", "#FF7F11")
                    .set("font-weight", "700")
                    .set("font-size", "2.5rem")
                    .set("margin", "0");

            Span companyInfo = new Span("Please select a company to continue");
            companyInfo.getStyle()
                    .set("color", "#6C757D")
                    .set("font-size", "1.1rem")
                    .set("margin-top", "0.5rem")
                    .set("display", "block");

            VerticalLayout headerContent = new VerticalLayout(header, companyInfo);
            headerContent.setSpacing(false);
            headerContent.setPadding(false);
            headerContent.setAlignItems(FlexComponent.Alignment.CENTER);

            headerContainer.add(headerContent);
        }

        add(headerContainer);
    }

    private void createStatsCards() {
        if (statsLayout == null) {
            statsLayout = new HorizontalLayout();
            statsLayout.setWidthFull();
            statsLayout.setSpacing(true);
            add(statsLayout);
        }

        statsLayout.removeAll();

        if (companyStats != null) {
            statsLayout.add(createStatCard("Total Branches", String.valueOf(companyStats.getTotalBranches()), VaadinIcon.STOCK, "primary"));
            statsLayout.add(createStatCard("Active Branches", String.valueOf(companyStats.getActiveBranches()), VaadinIcon.CHECK, "success"));
        } else if (currentCompany != null) {
            statsLayout.add(createStatCard("Total Branches", "Loading...", VaadinIcon.STOCK, "primary"));
            statsLayout.add(createStatCard("Active Branches", "Loading...", VaadinIcon.CHECK, "success"));
        } else {
            statsLayout.add(createStatCard("Total Branches", "0", VaadinIcon.STOCK, "primary"));
            statsLayout.add(createStatCard("Active Branches", "0", VaadinIcon.CHECK, "success"));
        }
    }

    private Div createStatCard(String title, String value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.getStyle()
                .set("padding", "1rem")
                .set("border-radius", "0.5rem")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("flex", "1")
                .set("min-width", "150px")
                .set("background", color.equals("primary") ? "#FF7F11" : "#0A9396")
                .set("color", "white");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(true);

        Span iconSpan = new Span(icon.create());
        iconSpan.getStyle().set("color", "white");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "rgba(255, 255, 255, 0.85)");

        header.add(iconSpan, titleSpan);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", "white");

        VerticalLayout content = new VerticalLayout(header, valueSpan);
        content.setSpacing(true);
        content.setPadding(false);

        card.add(content);
        return card;
    }

    private void createBranchSection() {
        H1 sectionHeader = new H1("Branches");
        sectionHeader.getStyle()
                .set("font-size", "1.5rem")
                .set("margin-top", "2rem")
                .set("margin-bottom", "1rem");

        addBranchButton = new Button("Add Branch", e -> showAddBranchDialog(null));
        addBranchButton.getStyle().set("background", "#FF7F11").set("color", "white");

        HorizontalLayout sectionToolbar = new HorizontalLayout(sectionHeader, addBranchButton);
        sectionToolbar.setWidthFull();
        sectionToolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        sectionToolbar.setAlignItems(Alignment.CENTER);

        add(sectionToolbar);
        createBranchGrid();
    }

    private void createBranchGrid() {
        branchGrid = new Grid<>(Branch.class, false);
        branchGrid.addColumn(Branch::getName).setHeader("Branch Name").setAutoWidth(true);
        branchGrid.addColumn(Branch::getCode).setHeader("Code").setAutoWidth(true);
        branchGrid.addColumn(Branch::getAddress).setHeader("Address").setAutoWidth(true);
        branchGrid.addColumn(Branch::getPhone).setHeader("Phone").setAutoWidth(true);
        branchGrid.addColumn(Branch::getEmail).setHeader("Email").setAutoWidth(true);
        branchGrid.addColumn(branch -> branch.isActive() ? "Active" : "Inactive").setHeader("Status").setAutoWidth(true);

        branchGrid.addComponentColumn(branch -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button editButton = new Button(VaadinIcon.EDIT.create(), e -> showAddBranchDialog(branch));
            editButton.getStyle().set("color", "#0A9396");
            Button deleteButton = new Button(VaadinIcon.TRASH.create(), e -> showDeleteConfirmationDialog(branch));
            deleteButton.getStyle().set("color", "#FF7F11");
            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        // ✅ Add modern styling and header theme
        branchGrid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_WRAP_CELL_CONTENT,
                GridVariant.LUMO_COMPACT
        );

        branchGrid.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background", "white")
                .set("--lumo-primary-text-color", "#0A9396");

        // ✅ Custom header styling (orange background with white text)
        branchGrid.getElement().executeJs("""
        const headerRows = this.shadowRoot.querySelectorAll('thead th');
        headerRows.forEach(th => {
            th.style.backgroundColor = '#FF7F11';
            th.style.color = 'white';
            th.style.fontWeight = '600';
            th.style.textAlign = 'center';
            th.style.borderRight = '1px solid rgba(255,255,255,0.2)';
        });
    """);

        // ✅ Add subtle hover effect for rows
        branchGrid.getElement().executeJs("""
        const rows = this.shadowRoot.querySelectorAll('tbody tr');
        rows.forEach(row => {
            row.addEventListener('mouseenter', () => row.style.backgroundColor = 'rgba(10,147,150,0.05)');
            row.addEventListener('mouseleave', () => row.style.backgroundColor = '');
        });
    """);

        if (currentCompany != null) {
            branchGrid.setItems(branchService.getBranchesByCompany(currentCompany.getId()));
        }

        add(branchGrid);
    }

    private void showAddBranchDialog(Branch branch) {
        boolean isEdit = branch != null;
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

        H1 dialogTitle = new H1(isEdit ? "Edit Branch" : "Add New Branch");
        dialogTitle.getStyle().set("color", "#0A9396").set("font-size", "1.5rem");

        TextField nameField = new TextField("Branch Name");
        TextField codeField = new TextField("Branch Code");
        TextField addressField = new TextField("Address");
        TextField phoneField = new TextField("Phone");
        TextField emailField = new TextField("Email");

        if (isEdit) {
            nameField.setValue(branch.getName());
            codeField.setValue(branch.getCode() != null ? branch.getCode() : "");
            addressField.setValue(branch.getAddress() != null ? branch.getAddress() : "");
            phoneField.setValue(branch.getPhone() != null ? branch.getPhone() : "");
            emailField.setValue(branch.getEmail() != null ? branch.getEmail() : "");
        }

        FormLayout form = new FormLayout(nameField, codeField, addressField, phoneField, emailField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveButton = new Button(isEdit ? "Update" : "Save", e -> {
            String name = nameField.getValue();
            String code = codeField.getValue();
            if (isEdit ? branchService.canUpdateBranch(branch.getId(), name, code, currentCompany.getId())
                    : branchService.branchNameOrCodeExists(currentCompany.getId(), name, code)) {
                Notification.show("Branch name or code already exists for this company!");
                return;
            }
            saveBranch(branch, name, code, addressField.getValue(), phoneField.getValue(), emailField.getValue(), isEdit);
            dialog.close();
        });
        saveButton.getStyle().set("background", "#0A9396").set("color", "white");

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        VerticalLayout dialogLayout = new VerticalLayout(dialogTitle, form, buttons);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void showDeleteConfirmationDialog(Branch branch) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        H1 dialogTitle = new H1("Confirm Deletion");
        dialogTitle.getStyle().set("color", "#FF7F11").set("font-size", "1.5rem");

        Span message = new Span("Are you sure you want to deactivate the branch '" + branch.getName() + "'?");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-m)");

        Button confirmButton = new Button("Delete", e -> {
            branchService.deactivateBranch(branch.getId());
            branchGrid.setItems(branchService.getActiveBranchesByCompany(currentCompany.getId()));
            branchService.getCompanyStats(currentCompany.getId()).ifPresent(dto -> {
                companyStats = dto;
                createStatsCards();
            });
            Notification.show("Branch deleted successfully!");
            dialog.close();
        });
        confirmButton.getStyle().set("background", "#FF7F11").set("color", "white");

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        buttons.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(dialogTitle, message, buttons);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void saveBranch(Branch branch, String name, String code, String address, String phone, String email, boolean isEdit) {
        try {
            Branch branchToSave = isEdit ? branch : new Branch();
            branchToSave.setName(name);
            branchToSave.setCode(code);
            branchToSave.setAddress(address);
            branchToSave.setPhone(phone);
            branchToSave.setEmail(email);
            branchToSave.setActive(true);

            if (!isEdit) {
                branchToSave.setCompany(currentCompany);
                branchService.createBranch(branchToSave, currentCompany);
            } else {
                branchService.updateBranch(branchToSave);
            }

            branchGrid.setItems(branchService.getActiveBranchesByCompany(currentCompany.getId()));
            branchService.getCompanyStats(currentCompany.getId()).ifPresent(dto -> {
                companyStats = dto;
                createStatsCards();
            });

            Notification.show(isEdit ? "Branch updated successfully!" : "Branch created successfully!");
        } catch (Exception e) {
            Notification.show("Error " + (isEdit ? "updating" : "creating") + " branch: " + e.getMessage());
        }
    }
}
