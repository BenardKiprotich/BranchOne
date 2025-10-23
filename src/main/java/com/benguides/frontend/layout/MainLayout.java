package com.benguides.frontend.layout;

import com.benguides.models.User;
import com.benguides.security.SecurityService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    private final SecurityService securityService;
    private final AccessAnnotationChecker accessChecker;
    private final User currentUser;

    public MainLayout(SecurityService securityService, AccessAnnotationChecker accessChecker) {
        this.securityService = securityService;
        this.accessChecker = accessChecker;

        this.currentUser = securityService.getAuthenticatedUser().orElse(null);

        setPrimarySection(Section.DRAWER);
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        // Company / Branch info
        String headerText = "BranchOne";
        if (currentUser != null && currentUser.getCompany() != null) {
            headerText = currentUser.getCompany().getName();
            if (currentUser.getBranch() != null) {
                headerText += " - " + currentUser.getBranch().getName();
            }
        }

        H1 title = new H1(headerText);
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("font-weight", "bold")
                .set("color", "white");

        HorizontalLayout headerLeft = new HorizontalLayout(toggle, title);
        headerLeft.setAlignItems(FlexComponent.Alignment.CENTER);

        // User menu (avatar + dropdown)
        Avatar avatar = new Avatar();
        if (currentUser != null) {
            avatar.setName(currentUser.getFirstName() + " " + currentUser.getLastName());
        }

        MenuBar userMenu = new MenuBar();
        SubMenu subMenu = userMenu.addItem(avatar).getSubMenu();
        subMenu.addItem("Profile", e -> UI.getCurrent().navigate("profile"));
        subMenu.addItem("Change Password", e -> UI.getCurrent().navigate("change-password"));
        subMenu.add(new Hr());
        subMenu.addItem("Logout", e -> securityService.logout());

        // Header layout styling
        HorizontalLayout header = new HorizontalLayout(headerLeft, userMenu);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #FFB97D, #0A9396)")
                .set("color", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.15)")
                .set("border-bottom", "2px solid rgba(255,255,255,0.15)");

        addToNavbar(header);
    }

    private void createDrawer() {
        // Drawer Header
        H2 appName = new H2("BranchOne");
        appName.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.FontWeight.BOLD
        );
        appName.getStyle()
                .set("color", "white")
                .set("margin", "0")
                .set("padding", "1rem 1.5rem")
                .set("background", "#FF7F11") // solid brand orange
                .set("border-bottom", "2px solid rgba(255,255,255,0.15)")
                .set("text-shadow", "0 1px 2px rgba(0,0,0,0.25)");

        // Navigation content
        SideNav nav = createNavigation();
        Scroller scroller = new Scroller(nav);
        scroller.addClassName("navigation-scroller");

        // Sidebar gradient background (starts below BranchOne)
        scroller.getStyle()
                .set("background", "linear-gradient(180deg, #FFB97D, #0A9396)")
                .set("color", "white")
                .set("height", "100%");

        addToDrawer(appName, scroller);
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        if (currentUser == null) {
            return nav;
        }

        // Common styling for all nav items
        nav.getStyle()
                .set("background", "transparent")
                .set("color", "white")
                .set("font-weight", "600");

        // Super Admin
        if (currentUser.hasRole("ROLE_SUPER_ADMIN")) {
            nav.addItem(styledNavItem("Dashboard", "super-admin/dashboard", VaadinIcon.DASHBOARD.create()));
            nav.addItem(styledNavItem("Companies", "super-admin/companies", VaadinIcon.BUILDING.create()));
            nav.addItem(styledNavItem("System Users", "super-admin/users", VaadinIcon.USERS.create()));
            nav.addItem(styledNavItem("Roles & Permissions", "super-admin/roles", VaadinIcon.SHIELD.create()));

            // Company Admin
        } else if (currentUser.hasRole("ROLE_COMPANY_ADMIN")) {
            nav.addItem(styledNavItem("Dashboard", "company-admin/dashboard", VaadinIcon.DASHBOARD.create()));

            SideNavItem inventory = new SideNavItem("Inventory");
            inventory.setPrefixComponent(VaadinIcon.PACKAGE.create());
            inventory.setPath((String) null); // Disable direct navigation

            SideNavItem products = styledNavItem("Products", "inventory", VaadinIcon.CUBE.create());
            inventory.addItem(products);
            nav.addItem(inventory);

            nav.addItem(styledNavItem("Sales", "company-admin/sales", VaadinIcon.MONEY_DEPOSIT.create()));
            nav.addItem(styledNavItem("Expenses", "company-admin/expenses", VaadinIcon.ARCHIVE.create()));
            nav.addItem(styledNavItem("Reports", "company-admin/reports", VaadinIcon.FILE_TEXT.create()));

            // Branch/Shift Roles
        } else if (securityService.hasAnyRole("ROLE_BRANCH_MANAGER", "ROLE_SHIFT_SUPERVISOR", "ROLE_SHIFT_ATTENDANT")) {
            nav.addItem(styledNavItem("Dashboard", "inventory", VaadinIcon.DASHBOARD.create()));
            nav.addItem(styledNavItem("Inventory", "inventory", VaadinIcon.PACKAGE.create()));
        }

        return nav;
    }

    private SideNavItem styledNavItem(String label, String path, com.vaadin.flow.component.icon.Icon icon) {
        SideNavItem item = new SideNavItem(label, path, icon);

        // Basic inline styles for look & spacing
        item.getStyle()
                .set("color", "white")
                .set("border-radius", "8px")
                .set("margin", "0.25rem 0.75rem")
                .set("padding", "0.5rem 1rem")
                .set("transition", "background 0.18s ease, transform 0.12s ease")
                .set("cursor", "pointer");

        // Hover effect via mouseover/mouseout to avoid using unavailable API
        item.getElement().addEventListener("mouseover", evt -> {
            item.getStyle().set("background", "rgba(255,255,255,0.08)");
            item.getStyle().set("transform", "translateY(-1px)");
        });

        item.getElement().addEventListener("mouseout", evt -> {
            item.getStyle().set("background", "transparent");
            item.getStyle().set("transform", "translateY(0)");
        });

        // Focus/active styling (keyboard/navigation) — keep simple
        item.getElement().addEventListener("focus", evt -> item.getStyle().set("background", "rgba(255,255,255,0.08)"));
        item.getElement().addEventListener("blur", evt -> item.getStyle().set("background", "transparent"));

        return item;
    }
}