package com.benguides.frontend.views.superadmin;

import com.benguides.dtos.UserDTO;
import com.benguides.frontend.layout.MainLayout;
import com.benguides.models.User;
import com.benguides.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "super-admin/users", layout = MainLayout.class)
@RolesAllowed("ROLE_SUPER_ADMIN")
public class SuperAdminUsersView extends VerticalLayout {

    private final UserService userService;
    private Grid<UserDTO> userGrid;

    @Autowired
    public SuperAdminUsersView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createUserGrid();
        loadUsers();
    }

    private void createHeader() {
        H1 header = new H1("System Users");
        header.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("margin-bottom", "2rem");
        add(header);
    }

    private void createUserGrid() {
        userGrid = new Grid<>(UserDTO.class, false);

        userGrid.addColumn(UserDTO::username).setHeader("Username").setAutoWidth(true);
        userGrid.addColumn(u -> u.firstName() + " " + u.lastName()).setHeader("Full Name").setAutoWidth(true);
        userGrid.addColumn(UserDTO::email).setHeader("Email").setAutoWidth(true);
        userGrid.addColumn(UserDTO::phoneNumber).setHeader("Phone").setAutoWidth(true);
        userGrid.addColumn(UserDTO::companyName).setHeader("Company").setAutoWidth(true);
        userGrid.addColumn(UserDTO::branchName).setHeader("Branch").setAutoWidth(true);
        userGrid.addColumn(u -> String.join(", ", u.roles())).setHeader("Roles").setAutoWidth(true);
        userGrid.addColumn(u -> u.status().name()).setHeader("Status").setAutoWidth(true);

        // You can still add buttons if needed, but you’ll need to fetch User entity for editing
        userGrid.setHeight("500px");
        add(userGrid);
    }

    private Button createEditButton(User user) {
        Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(e -> openEditDialog(user));
        return editButton;
    }

    private void openEditDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit User");

        TextField firstName = new TextField("First Name");
        firstName.setValue(user.getFirstName() != null ? user.getFirstName() : "");

        TextField lastName = new TextField("Last Name");
        lastName.setValue(user.getLastName() != null ? user.getLastName() : "");

        EmailField email = new EmailField("Email");
        email.setValue(user.getEmail() != null ? user.getEmail() : "");
        email.setPlaceholder("Enter email address");

        FormLayout form = new FormLayout(firstName, lastName, email);

        Button saveButton = new Button("Save", event -> {
            user.setFirstName(firstName.getValue());
            user.setLastName(lastName.getValue());
            user.setEmail(email.getValue());
            userService.save(user);
            Notification.show("User updated successfully!", 3000, Notification.Position.MIDDLE);
            loadUsers();
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setSpacing(true);

        dialog.add(form, actions);
        dialog.setWidth("400px");
        dialog.open();
    }

    private void loadUsers() {
        List<UserDTO> users = userService.getAllUsersAsDTOs();
        userGrid.setItems(users);
    }
}
