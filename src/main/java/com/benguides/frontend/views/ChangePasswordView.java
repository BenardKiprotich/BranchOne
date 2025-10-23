package com.benguides.frontend.views;

import com.benguides.models.User;
import com.benguides.services.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("change-password")
@jakarta.annotation.security.PermitAll
public class ChangePasswordView extends VerticalLayout {

    private final UserService userService;

    public ChangePasswordView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setWidth("400px");
        formLayout.setPadding(true);
        formLayout.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        formLayout.getStyle().set("border-radius", "12px");
        formLayout.getStyle().set("background", "var(--lumo-base-color)");

        add(new H2("Change Password (First-time Login)"));

        PasswordField newPassword = new PasswordField("New Password");
        newPassword.setWidthFull();
        newPassword.setRequired(true);

        PasswordField confirmPassword = new PasswordField("Confirm Password");
        confirmPassword.setWidthFull();
        confirmPassword.setRequired(true);

        Button save = new Button("Save New Password", ev -> {
            if (!newPassword.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Passwords do not match");
                return;
            }

            if (newPassword.getValue().length() < 6) {
                Notification.show("Password must be at least 6 characters");
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                Notification.show("Not authenticated");
                return;
            }

            String username = auth.getName();
            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                Notification.show("User not found");
                return;
            }

            userService.changePassword(user.getId(), newPassword.getValue());
            Notification.show("Password changed successfully! Redirecting...");

            // Redirect user based on their role
            String redirectUrl = "/";
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
                redirectUrl = "/super-admin/dashboard";
            } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY_ADMIN"))) {
                redirectUrl = "/company-admin/dashboard";
            } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BRANCH_MANAGER"))) {
                redirectUrl = "/branch-manager/dashboard";
            } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SHIFT_SUPERVISOR"))) {
                redirectUrl = "/shift-supervisor/dashboard";
            }

            // Delay a bit to let notification show
            UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => window.location.href = $0, 1000)", redirectUrl
            );
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        formLayout.add(newPassword, confirmPassword, save);
        add(formLayout);
    }
}
