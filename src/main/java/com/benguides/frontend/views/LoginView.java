package com.benguides.frontend.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("login")
@PermitAll
@PageTitle("Login | BranchOne")
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

    public LoginView() {
        setOpened(true);
        setAction("login");
        setTitle("BranchOne");
        setDescription("Multi-Branch Sales Management System");

        // Apply custom styling
        getElement().getThemeList().add("dark");
        getElement().getStyle().set("--lumo-primary-color", "#2563eb");
        getElement().getStyle().set("--lumo-primary-contrast-color", "#ffffff");

        LoginI18n i18n = LoginI18n.createDefault();

        // Customize the header
        i18n.setHeader(new LoginI18n.Header());
        i18n.getHeader().setTitle("BranchOne");
        i18n.getHeader().setDescription("Sign in to your account");

        // Customize the form
        i18n.getForm().setUsername("Username");
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Sign In");
        i18n.getForm().setForgotPassword("Forgot password?");
        i18n.getForm().setTitle("Login");

        // Customize error messages
        i18n.getErrorMessage().setTitle("Login failed");
        i18n.getErrorMessage().setMessage(
                "Please check your username and password. " +
                        "Make sure your account is active and not locked."
        );

        // Additional messages
//        LoginI18n.AdditionalMessage additionalMessage = new LoginI18n.AdditionalMessage();
//        additionalMessage.setText("Need help? Contact your system administrator.");
//        i18n.setAdditionalMessage(additionalMessage);

        setI18n(i18n);

        // Enable forgot password feature
        setForgotPasswordButtonVisible(true);
        addForgotPasswordListener(e -> {
            // For now, just show a message. You can implement the actual flow later
            UI.getCurrent().getPage().executeJs(
                    "alert('Please contact your system administrator to reset your password.');"
            );
        });

        // Add some custom styling
        getElement().executeJs(
                "this.$.vaadinLoginOverlayWrapper.style.background = " +
                        "'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';" +
                        "this.$.vaadinLoginOverlayWrapper.style.display = 'flex';" +
                        "this.$.vaadinLoginOverlayWrapper.style.alignItems = 'center';" +
                        "this.$.vaadinLoginOverlayWrapper.style.justifyContent = 'center';"
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Handle logout parameter
        if (event.getLocation().getQueryParameters().getParameters().containsKey("logout")) {
            setDescription("You have been logged out successfully.");
            setOpened(true);
        }

        // Handle error parameter
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            setError(true);
            setOpened(true);
        }

        // Check if user is already authenticated and redirect accordingly
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            String redirectUrl = determineRedirectUrl(authentication);
            event.forwardTo(redirectUrl);
        }
    }

    private String determineRedirectUrl(Authentication authentication) {
        // Default redirect for shift attendants
        String redirectUrl = "/shift-attendant";

        // Check user's roles and redirect to appropriate dashboard
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();

            switch (role) {
                case "ROLE_SUPER_ADMIN":
                    redirectUrl = "/super-admin/dashboard";
                    break;
                case "ROLE_COMPANY_ADMIN":
                    redirectUrl = "/company-admin/dashboard";
                    break;
                case "ROLE_BRANCH_MANAGER":
                    redirectUrl = "/branch-manager/dashboard";
                    break;
                case "ROLE_SHIFT_SUPERVISOR":
                    redirectUrl = "/shift-supervisor/dashboard";
                    break;
                case "ROLE_SHIFT_ATTENDANT":
                    redirectUrl = "/shift-attendant/dashboard";
                    break;
            }

            // If we found a specific role, break out of the loop
            if (!redirectUrl.equals("/shift-attendant")) {
                break;
            }
        }

        return redirectUrl;
    }
}