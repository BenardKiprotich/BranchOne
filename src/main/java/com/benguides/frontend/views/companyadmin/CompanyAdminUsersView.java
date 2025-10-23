package com.benguides.frontend.views.companyadmin;

import com.benguides.dtos.UserDTO;
import com.benguides.dtos.UserWithCompanyDTO;
import com.benguides.frontend.layout.MainLayout;
import com.benguides.models.Branch;
import com.benguides.models.Role;
import com.benguides.models.User;
import com.benguides.security.SecurityService;
import com.benguides.services.BranchService;
import com.benguides.services.RoleService;
import com.benguides.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "company-admin/users", layout = MainLayout.class)
@RolesAllowed("ROLE_COMPANY_ADMIN")
public class CompanyAdminUsersView extends VerticalLayout {

    private final UserService userService;
    private final BranchService branchService;
    private final RoleService roleService;
    private final SecurityService securityService;

    private Grid<UserDTO> userGrid;
    private List<Branch> companyBranches;
    private List<Role> availableRoles;
    private UserWithCompanyDTO currentUser;

    @Autowired
    public CompanyAdminUsersView(UserService userService, BranchService branchService,
                                 RoleService roleService, SecurityService securityService) {
        this.userService = userService;
        this.branchService = branchService;
        this.roleService = roleService;
        this.securityService = securityService;

        this.currentUser = securityService.getAuthenticatedUserWithCompany().orElse(null);

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        loadCompanyData();
        createHeader();
        createUserGrid();
        loadUsers();
    }

    private void loadCompanyData() {
        if (currentUser != null && currentUser.companyId() != null) {
            Long companyId = currentUser.companyId();
            // Use DTOs instead of entities
            this.companyBranches = branchService.getBranchesByCompany(companyId)
                    .stream()
                    .map(branch -> {
                        Branch b = new Branch();
                        b.setId(branch.getId());
                        b.setName(branch.getName());
                        return b;
                    })
                    .toList();

            this.availableRoles = roleService.getAllRoles().stream()
                    .filter(role -> !role.getName().equals("ROLE_SUPER_ADMIN"))
                    .collect(Collectors.toList());
        }
    }

    private void createHeader() {
        H1 header = new H1("Company Users");
        header.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("margin-bottom", "1rem");

        Button addUserButton = new Button("Add User", VaadinIcon.PLUS.create());
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUserButton.addClickListener(e -> openAddUserDialog());

        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(e -> loadUsers());

        HorizontalLayout headerLayout = new HorizontalLayout(header, addUserButton, refreshButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        add(headerLayout);
    }

    private void createUserGrid() {
        userGrid = new Grid<>(UserDTO.class, false);

        userGrid.addColumn(UserDTO::username).setHeader("Username").setAutoWidth(true);
        userGrid.addColumn(u -> u.firstName() + " " + u.lastName()).setHeader("Full Name").setAutoWidth(true);
        userGrid.addColumn(UserDTO::email).setHeader("Email").setAutoWidth(true);
        userGrid.addColumn(UserDTO::phoneNumber).setHeader("Phone").setAutoWidth(true);
        userGrid.addColumn(UserDTO::branchName).setHeader("Branch").setAutoWidth(true);
        userGrid.addColumn(u -> String.join(", ", u.roles())).setHeader("Roles").setAutoWidth(true);
        userGrid.addColumn(u -> u.status().name()).setHeader("Status").setAutoWidth(true);

        userGrid.addComponentColumn(userDTO -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            editButton.addClickListener(e -> openEditUserDialog(userDTO));

            Button rolesButton = new Button("Roles", VaadinIcon.SHIELD.create());
            rolesButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            rolesButton.addClickListener(e -> openRolesDialog(userDTO));

            Button toggleButton = new Button(userDTO.status() == User.Status.ACTIVE ? "Disable" : "Enable",
                    userDTO.status() == User.Status.ACTIVE ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            toggleButton.addClickListener(e -> toggleUserStatus(userDTO));

            actions.add(editButton, rolesButton, toggleButton);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        userGrid.setHeight("500px");
        add(userGrid);
    }

    private void openAddUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New User");

        // Fetch current company admin
        User companyAdmin = securityService.getAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (companyBranches.isEmpty()) {
            Notification.show("No branches enrolled yet. Please create a branch first.", 5000, Notification.Position.MIDDLE);
            return; // block registration
        }

        // Form fields
        TextField username = new TextField("Username");
        username.setRequired(true);

        TextField firstName = new TextField("First Name");
        TextField lastName = new TextField("Last Name");

        EmailField email = new EmailField("Email");
        email.setRequired(true);

        TextField phoneNumber = new TextField("Phone Number");

        PasswordField password = new PasswordField("Password");
        password.setValue("Temp@123"); // default temp password
        password.setReadOnly(true); // optional: prevent editing

        ComboBox<Branch> branchComboBox = new ComboBox<>("Branch");
        branchComboBox.setItems(companyBranches);
        branchComboBox.setItemLabelGenerator(Branch::getName);
        branchComboBox.setRequired(true);

        // Roles selection (multi-checkbox)
        VerticalLayout rolesLayout = new VerticalLayout();
        rolesLayout.setSpacing(false);
        Set<Role> selectedRoles = new HashSet<>();
        for (Role role : availableRoles) {
            Checkbox checkbox = new Checkbox(role.getDisplayName() + " (" + role.getName() + ")");
            if ("ROLE_SHIFT_ATTENDANT".equals(role.getName())) {
                checkbox.setValue(true); // preselect basic role
                selectedRoles.add(role);
            }
            checkbox.addValueChangeListener(e -> {
                if (e.getValue()) selectedRoles.add(role);
                else selectedRoles.remove(role);
            });
            rolesLayout.add(checkbox);
        }

        Select<User.Status> statusSelect = new Select<>();
        statusSelect.setLabel("Status");
        statusSelect.setItems(User.Status.ACTIVE, User.Status.DISABLED);
        statusSelect.setValue(User.Status.ACTIVE);

        FormLayout formLayout = new FormLayout();
        formLayout.add(username, firstName, lastName, email, phoneNumber, password, branchComboBox, statusSelect);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Save button
        Button saveButton = new Button("Save", e -> {
            if (username.isEmpty() || email.isEmpty() || branchComboBox.getValue() == null) {
                Notification.show("Username, email, and branch are required!", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (selectedRoles.isEmpty()) {
                Notification.show("User must have at least one role!", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                User newUser = new User();
                newUser.setUsername(username.getValue());
                newUser.setFirstName(firstName.getValue());
                newUser.setLastName(lastName.getValue());
                newUser.setEmail(email.getValue());
                newUser.setPhoneNumber(phoneNumber.getValue());
                newUser.setPassword(password.getValue());
                newUser.setStatus(statusSelect.getValue());
                newUser.setCompany(companyAdmin.getCompany()); // assign company automatically
                newUser.setBranch(branchComboBox.getValue());
                newUser.setMustChangePassword(true);

                userService.createUser(newUser, selectedRoles, companyAdmin.getCompany(), branchComboBox.getValue());

                Notification.show("User created successfully!", 3000, Notification.Position.MIDDLE);
                loadUsers();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Error creating user: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);

        dialog.add(formLayout, rolesLayout, buttons);
        dialog.setWidth("600px");
        dialog.open();
    }
    private void openEditUserDialog(UserDTO userDTO) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit User: " + userDTO.username());

        // Fetch full user entity with details
        User user = userService.findByUsernameWithDetails(userDTO.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Form fields
        TextField username = new TextField("Username");
        username.setValue(user.getUsername());
        username.setRequired(true);

        TextField firstName = new TextField("First Name");
        firstName.setValue(user.getFirstName() != null ? user.getFirstName() : "");

        TextField lastName = new TextField("Last Name");
        lastName.setValue(user.getLastName() != null ? user.getLastName() : "");

        EmailField email = new EmailField("Email");
        email.setValue(user.getEmail() != null ? user.getEmail() : "");
        email.setRequired(true);

        TextField phoneNumber = new TextField("Phone Number");
        phoneNumber.setValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

        ComboBox<Branch> branchComboBox = new ComboBox<>("Branch");
        branchComboBox.setItems(companyBranches);
        branchComboBox.setItemLabelGenerator(Branch::getName);
        branchComboBox.setValue(user.getBranch());

        Select<User.Status> statusSelect = new Select<>();
        statusSelect.setLabel("Status");
        statusSelect.setItems(User.Status.values());
        statusSelect.setValue(user.getStatus());

        // Roles multi-checkbox
        VerticalLayout rolesLayout = new VerticalLayout();
        rolesLayout.setSpacing(false);
        Set<Role> selectedRoles = new HashSet<>(user.getRoles());
        for (Role role : availableRoles) {
            Checkbox checkbox = new Checkbox(role.getDisplayName() + " (" + role.getName() + ")");
            checkbox.setValue(user.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId())));
            checkbox.addValueChangeListener(e -> {
                if (e.getValue()) selectedRoles.add(role);
                else selectedRoles.remove(role);
            });
            rolesLayout.add(checkbox);
        }

        FormLayout formLayout = new FormLayout();
        formLayout.add(username, firstName, lastName, email, phoneNumber, branchComboBox, statusSelect);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Save button
        Button saveButton = new Button("Save", e -> {
            if (username.isEmpty() || email.isEmpty() || branchComboBox.getValue() == null) {
                Notification.show("Username, email, and branch are required!", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (selectedRoles.isEmpty()) {
                Notification.show("User must have at least one role!", 3000, Notification.Position.MIDDLE);
                return;
            }

            // Update user
            user.setUsername(username.getValue());
            user.setFirstName(firstName.getValue());
            user.setLastName(lastName.getValue());
            user.setEmail(email.getValue());
            user.setPhoneNumber(phoneNumber.getValue());
            user.setBranch(branchComboBox.getValue());
            user.setStatus(statusSelect.getValue());
            user.setRoles(selectedRoles);

            userService.save(user);
            Notification.show("User updated successfully!", 3000, Notification.Position.MIDDLE);
            loadUsers();
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);

        dialog.add(formLayout, rolesLayout, buttons);
        dialog.setWidth("600px");
        dialog.open();
    }

    private void openRolesDialog(UserDTO userDTO) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Manage Roles for: " + userDTO.username());

        VerticalLayout rolesLayout = new VerticalLayout();
        rolesLayout.setSpacing(true);
        rolesLayout.setPadding(false);

        // Fetch full user entity
        User user = userService.findByUsername(userDTO.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<Role> currentRoles = new HashSet<>(user.getRoles());
        Set<Checkbox> roleCheckboxes = new HashSet<>();

        for (Role role : availableRoles) {
            Checkbox checkbox = new Checkbox(role.getDisplayName() + " (" + role.getName() + ")");
            checkbox.setValue(currentRoles.stream().anyMatch(r -> r.getId().equals(role.getId())));
            roleCheckboxes.add(checkbox);
            rolesLayout.add(checkbox);
        }

        Button saveButton = new Button("Save Roles", e -> {
            Set<Role> selectedRoles = roleCheckboxes.stream()
                    .filter(Checkbox::getValue)
                    .map(checkbox -> availableRoles.stream()
                            .filter(role -> checkbox.getLabel().contains(role.getName()))
                            .findFirst()
                            .orElse(null))
                    .filter(role -> role != null)
                    .collect(Collectors.toSet());

            if (selectedRoles.isEmpty()) {
                Notification.show("User must have at least one role!", 3000, Notification.Position.MIDDLE);
                return;
            }

            user.setRoles(selectedRoles);
            userService.save(user);
            Notification.show("Roles updated successfully!", 3000, Notification.Position.MIDDLE);
            loadUsers();
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        dialog.add(rolesLayout, buttons);
        dialog.setWidth("400px");
        dialog.open();
    }

    private void toggleUserStatus(UserDTO userDTO) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirm Action");

        boolean willDisable = userDTO.status() == User.Status.ACTIVE;
        String action = willDisable ? "disable" : "enable";

        confirmDialog.setText("Are you sure you want to " + action + " user " + userDTO.username() + "?");

        confirmDialog.setConfirmText("Yes, " + action);
        confirmDialog.setConfirmButtonTheme("primary error");
        confirmDialog.setCancelable(true);

        confirmDialog.addConfirmListener(e -> {
            User user = userService.findByUsername(userDTO.username())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setStatus(willDisable ? User.Status.DISABLED : User.Status.ACTIVE);
            userService.save(user);

            Notification.show("User " + action + "d successfully!", 3000, Notification.Position.MIDDLE);
            loadUsers();
        });

        confirmDialog.open();
    }

    private void loadUsers() {
        if (currentUser != null && currentUser.companyId() != null) {
            // Fetch DTOs from the service, not entities
            List<UserDTO> users = userService.getUsersByCompany(currentUser.companyId())
                    .stream()
                    .map(user -> new UserDTO(
                            user.getId(),
                            user.getUsername(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getPhoneNumber(),
                            currentUser.companyName(), // use DTO instead of lazy-loaded entity
                            user.getBranch() != null ? user.getBranch().getName() : null,
                            user.getRoles().stream()
                                    .map(Role::getName)
                                    .sorted()
                                    .toList(),
                            user.getStatus()
                    ))
                    .toList();
            userGrid.setItems(users);
        }
    }
}
