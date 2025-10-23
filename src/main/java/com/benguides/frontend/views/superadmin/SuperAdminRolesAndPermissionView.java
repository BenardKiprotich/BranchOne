package com.benguides.frontend.views.superadmin;

import com.benguides.frontend.layout.MainLayout;
import com.benguides.models.Permission;
import com.benguides.models.Role;
import com.benguides.services.PermissionService;
import com.benguides.services.RoleService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Route(value = "super-admin/roles", layout = MainLayout.class)
@PageTitle("Roles & Permissions | BranchOne")
@RolesAllowed("ROLE_SUPER_ADMIN")
public class SuperAdminRolesAndPermissionView extends VerticalLayout {

    private final RoleService roleService;
    private final PermissionService permissionService;

    private Grid<Role> roleGrid;
    private Grid<Permission> permissionGrid;
    private TextField searchField;

    private List<Permission> allAllowedPermissions;
    private Role selectedRole;

    private final Map<String, Checkbox> permissionCheckboxes = new HashMap<>();
    private H2 selectedRoleLabel;
    private Button saveAllButton;

    @Autowired
    public SuperAdminRolesAndPermissionView(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(createHeader());
        add(createRoleSection());
        add(createPermissionSection());
        loadRoles();
    }

    private H1 createHeader() {
        H1 header = new H1("Roles & Permissions Management");
        header.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("margin-bottom", "1rem");
        return header;
    }

    // ========================= ROLE SECTION ========================= //
    private VerticalLayout createRoleSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(false);
        section.setSpacing(true);

        H2 title = new H2("Available Roles");

        Button addRoleButton = new Button("New Role", VaadinIcon.PLUS.create());
        addRoleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addRoleButton.addClickListener(e -> openAddRoleDialog());

        Button refreshButton = new Button(VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadRoles());

        HorizontalLayout titleBar = new HorizontalLayout(title, new HorizontalLayout(addRoleButton, refreshButton));
        titleBar.setWidthFull();
        titleBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        titleBar.setAlignItems(Alignment.CENTER);

        roleGrid = new Grid<>(Role.class, false);
        roleGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        roleGrid.addColumn(Role::getDisplayName).setHeader("Display Name").setAutoWidth(true);
        roleGrid.addColumn(Role::getName).setHeader("System Name").setAutoWidth(true);
        roleGrid.addColumn(role -> role.getPermissions().size()).setHeader("Permissions Count").setAutoWidth(true);

        roleGrid.addColumn(new ComponentRenderer<>(role -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> openEditRoleDialog(role));

            Button selectButton = new Button("Select", VaadinIcon.CHECK.create());
            selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            selectButton.addClickListener(e -> {
                roleGrid.select(role);
                onRoleSelected(role);
            });

            actions.add(selectButton, editButton);
            return actions;
        })).setHeader("Actions").setAutoWidth(true);

        roleGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) onRoleSelected(e.getValue());
        });

        roleGrid.setHeight("280px");

        section.add(titleBar, roleGrid);
        return section;
    }

    // ========================= PERMISSION SECTION ========================= //
    private VerticalLayout createPermissionSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(false);
        section.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        H2 title = new H2("Role Permissions");
        selectedRoleLabel = new H2("(No role selected)");
        selectedRoleLabel.getStyle().set("color", "var(--lumo-primary-color)");
        header.add(title, selectedRoleLabel);

        //old
//        searchField = new TextField();
//        searchField.setPlaceholder("Search permissions...");
//        searchField.setClearButtonVisible(true);
//        searchField.setWidth("50%");
//        searchField.addValueChangeListener(e -> filterPermissions(e.getValue()));
        //
        searchField = new TextField();
        searchField.setPlaceholder("Search permissions...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("50%");
        searchField.setValueChangeMode(ValueChangeMode.EAGER); // 🔹 triggers on every key release
        searchField.addValueChangeListener(e -> {
            UI ui = UI.getCurrent();
            String value = e.getValue();
            CompletableFuture.delayedExecutor(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> ui.access(() -> filterPermissions(value)));
        });

        Button selectAll = new Button("Select All", VaadinIcon.CHECK_SQUARE.create());
        Button unselectAll = new Button("Unselect All", VaadinIcon.CLOSE_SMALL.create());

        selectAll.addClickListener(e -> toggleAllCheckboxes(true));
        unselectAll.addClickListener(e -> toggleAllCheckboxes(false));

        HorizontalLayout bulkActions = new HorizontalLayout(searchField, selectAll, unselectAll);
        bulkActions.setWidthFull();
        bulkActions.setAlignItems(Alignment.END);
        bulkActions.setJustifyContentMode(JustifyContentMode.BETWEEN);

        permissionGrid = new Grid<>(Permission.class, false);
        permissionGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        permissionGrid.setHeight("500px");

        permissionGrid.addColumn(new ComponentRenderer<>(permission -> {
            Span tag = new Span(permission.getCategory());
            tag.getElement().getThemeList().add("badge small");
            tag.getStyle().set("background-color", switch (permission.getCategory()) {
                case "USER_MANAGEMENT" -> "#E0F7FA";
                case "COMPANY_MANAGEMENT" -> "#FFF3E0";
                case "SYSTEM_ADMIN" -> "#E8F5E9";
                default -> "#ECEFF1";
            });
            tag.getStyle().set("border-radius", "var(--lumo-border-radius-s)")
                    .set("padding", "0.2em 0.6em");
            return tag;
        })).setHeader("Category").setAutoWidth(true);

        permissionGrid.addColumn(Permission::getName).setHeader("Name").setAutoWidth(true);
        permissionGrid.addColumn(Permission::getDescription).setHeader("Description").setAutoWidth(true);

        permissionGrid.addColumn(new ComponentRenderer<>(permission -> {
            Checkbox checkbox = new Checkbox();
            permissionCheckboxes.put(permission.getName(), checkbox);

            checkbox.addValueChangeListener(e -> onPermissionToggle(permission, e.getValue()));
            if (selectedRole != null && selectedRole.getPermissions().stream()
                    .anyMatch(p -> p.getName().equals(permission.getName()))) {
                checkbox.setValue(true);
            }
            return checkbox;
        })).setHeader("Enabled").setAutoWidth(true);

        saveAllButton = new Button("Save All Changes", VaadinIcon.CHECK.create());
        saveAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveAllButton.setEnabled(false);
        saveAllButton.addClickListener(e -> savePermissionsAsync());

        section.add(header, bulkActions, permissionGrid, saveAllButton);
        return section;
    }

    // ========================= LOGIC ========================= //
    private void loadRoles() {
        roleGrid.setItems(roleService.getAllRoles());
    }

    private void onRoleSelected(Role role) {
        this.selectedRole = role;
        selectedRoleLabel.setText(role.getDisplayName() + " (" + role.getName() + ")");
        saveAllButton.setEnabled(false);

        List<String> allowedCategories = List.of("USER_MANAGEMENT", "COMPANY_MANAGEMENT", "SYSTEM_ADMIN");
        allAllowedPermissions = permissionService.getAllPermissions().stream()
                .filter(p -> allowedCategories.contains(p.getCategory()))
                .collect(Collectors.toList());

        permissionGrid.setItems(allAllowedPermissions);

        // Delay update to ensure grid & checkboxes render first
        UI.getCurrent().access(() ->
                UI.getCurrent().getPage().executeJs("setTimeout(() => $0.click(), 200);", getElement())
        );
        getUI().ifPresent(ui -> ui.access(() -> updatePermissionCheckboxes(role)));
    }

    private void updatePermissionCheckboxes(Role role) {
        if (role == null) return;
        Set<String> rolePermissionNames = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        permissionCheckboxes.forEach((name, checkbox) ->
                checkbox.setValue(rolePermissionNames.contains(name))
        );
    }

    private void toggleAllCheckboxes(boolean value) {
        permissionCheckboxes.values().forEach(cb -> cb.setValue(value));
        saveAllButton.setEnabled(true);
    }

    private void filterPermissions(String filter) {
        if (allAllowedPermissions == null) return;

        String lower = filter.toLowerCase();
        List<Permission> filtered = allAllowedPermissions.stream()
                .filter(p -> p.getName().toLowerCase().contains(lower)
                        || p.getDescription().toLowerCase().contains(lower)
                        || p.getCategory().toLowerCase().contains(lower))
                .collect(Collectors.toList());

        permissionGrid.setItems(filtered);
        updatePermissionCheckboxes(selectedRole);
    }

    private void onPermissionToggle(Permission permission, boolean enabled) {
        if (selectedRole == null) return;
        saveAllButton.setEnabled(true);

        if (enabled) {
            if (selectedRole.getPermissions().stream()
                    .noneMatch(p -> p.getName().equals(permission.getName()))) {
                selectedRole.getPermissions().add(permission);
            }
        } else {
            selectedRole.getPermissions().removeIf(p -> p.getName().equals(permission.getName()));
        }
    }

    private void savePermissionsAsync() {
        if (selectedRole == null) {
            Notification.show("Please select a role first.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Notification.show("Saving permissions...", 1000, Notification.Position.MIDDLE);

        CompletableFuture.runAsync(() -> {
            try {
                Role saved = roleService.save(selectedRole);
                UI.getCurrent().access(() -> {
                    Notification.show("Permissions saved successfully for " + saved.getDisplayName() + "!", 3000, Notification.Position.MIDDLE);
                    loadRoles();
                    saveAllButton.setEnabled(false);
                });
            } catch (Exception ex) {
                UI.getCurrent().access(() ->
                        Notification.show("Error saving permissions: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                );
            }
        });
    }

    // ========================= DIALOGS ========================= //
    private void openAddRoleDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create New Role");

        TextField displayName = new TextField("Display Name");
        TextField systemName = new TextField("System Name");

        Button saveButton = new Button("Save", e -> {
            if (displayName.isEmpty() || systemName.isEmpty()) {
                Notification.show("Both fields are required!", 3000, Notification.Position.MIDDLE);
                return;
            }
            Role newRole = new Role();
            newRole.setDisplayName(displayName.getValue());
            newRole.setName(systemName.getValue());
            roleService.save(newRole);
            Notification.show("Role created successfully!");
            loadRoles();
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.add(new FormLayout(displayName, systemName), new HorizontalLayout(saveButton, cancelButton));
        dialog.setWidth("400px");
        dialog.open();
    }

    private void openEditRoleDialog(Role role) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Role: " + role.getDisplayName());

        TextField displayName = new TextField("Display Name", role.getDisplayName());
        Checkbox revokable = new Checkbox("Revokable", role.isRevokable());
        Checkbox resignable = new Checkbox("Resignable", role.isResignable());

        Button save = new Button("Save", e -> {
            role.setDisplayName(displayName.getValue());
            role.setRevokable(revokable.getValue());
            role.setResignable(resignable.getValue());
            roleService.save(role);
            Notification.show("Role updated successfully!");
            loadRoles();
            dialog.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.add(new FormLayout(displayName, revokable, resignable),
                new HorizontalLayout(save, cancel));
        dialog.setWidth("400px");
        dialog.open();
    }
}
