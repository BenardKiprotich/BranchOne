package com.benguides.frontend.views.superadmin;

import com.benguides.frontend.layout.MainLayout;
import com.benguides.models.Company;
import com.benguides.models.User;
import com.benguides.security.SecurityService;
import com.benguides.services.CompanyService;
import com.benguides.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Route(value = "super-admin/companies", layout = MainLayout.class)
@RolesAllowed("ROLE_SUPER_ADMIN")
public class CompanyManagementView extends VerticalLayout {

    private final CompanyService companyService;
    private final UserService userService;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;

    private Grid<Company> companyGrid;
    private Button addCompanyButton;
    private Dialog companyDialog;

    // Form fields
    private TextField companyName;
    private TextField companyAddress;
    private ComboBox<String> countryCode;
    private TextField companyPhoneNumber;
    private EmailField companyEmail;
    private TextField adminFirstName;
    private TextField adminLastName;
    private TextField adminUsername;
    private EmailField adminEmail;
    private PasswordField adminPassword;

    // Binders for validation
    private Binder<Company> companyBinder;
    private Binder<User> userBinder;

    private Company currentCompany;

    // Country codes
    private final List<String> countryCodes = Arrays.asList("+254", "+255", "+256", "+257", "+250", "+211", "+253", "+252", "+251", "+27");

    @Autowired
    public CompanyManagementView(CompanyService companyService, UserService userService,
                                 SecurityService securityService, PasswordEncoder passwordEncoder) {
        this.companyService = companyService;
        this.userService = userService;
        this.securityService = securityService;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createToolbar();
        createCompanyGrid();
        createCompanyDialog();
        loadCompanies();
    }

    private void createHeader() {
        H1 header = new H1("Company Management");
        header.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold")
                .set("margin-bottom", "1rem");
        add(header);
    }

    private void createToolbar() {
        addCompanyButton = new Button("Add Company", e -> showCompanyDialog(null));
        addCompanyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addCompanyButton.setIcon(VaadinIcon.PLUS.create());

        HorizontalLayout toolbar = new HorizontalLayout(addCompanyButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.END);
        add(toolbar);
    }

    private void createCompanyGrid() {
        companyGrid = new Grid<>(Company.class, false);
        companyGrid.addColumn(Company::getName).setHeader("Company Name").setAutoWidth(true);
        companyGrid.addColumn(Company::getEmail).setHeader("Email").setAutoWidth(true);
        companyGrid.addColumn(Company::getPhone).setHeader("Phone").setAutoWidth(true);
        companyGrid.addColumn(Company::getAddress).setHeader("Address").setAutoWidth(true);
        companyGrid.addColumn(Company::getSubscriptionPlan).setHeader("Plan").setAutoWidth(true);
        companyGrid.addColumn(company -> company.isActive() ? "Active" : "Inactive")
                .setHeader("Status").setAutoWidth(true);

        companyGrid.addComponentColumn(company -> {
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addClickListener(e -> showCompanyDialog(company));
            editButton.setTooltipText("Edit Company");
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return editButton;
        }).setHeader("Actions").setAutoWidth(true);

        companyGrid.setHeight("500px");
        add(companyGrid);
    }

    private void createCompanyDialog() {
        companyDialog = new Dialog();
        companyDialog.setWidth("900px");
        companyDialog.getElement().getStyle().set("max-width", "90vw");

        initializeFormFields();
        setupValidation();

        // Phone number layout with country code
        HorizontalLayout phoneLayout = new HorizontalLayout(countryCode, companyPhoneNumber);
        phoneLayout.setWidthFull();
        phoneLayout.setSpacing(true);
        phoneLayout.setAlignItems(Alignment.END);
        phoneLayout.getStyle().set("margin-bottom", "15px");
        phoneLayout.setFlexGrow(0, countryCode);
        phoneLayout.setFlexGrow(1, companyPhoneNumber);

        FormLayout companyForm = new FormLayout();
        companyForm.getStyle().set("row-gap", "20px");
        companyForm.getStyle().set("column-gap", "30px");
        companyForm.add(companyName, companyAddress, phoneLayout, companyEmail);
        companyForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        FormLayout adminForm = new FormLayout();
        companyForm.getStyle().set("row-gap", "20px");
        companyForm.getStyle().set("column-gap", "30px");
        adminForm.add(adminFirstName, adminLastName, adminUsername, adminEmail, adminPassword);
        adminForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        Button saveButton = new Button("Save", e -> saveCompany());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> {
            companyDialog.close();
            resetForm();
        });
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);
    }

//    private void initializeFormFields() {
//        companyName = new TextField("Company Name");
//        companyName.setRequired(true);
//        companyName.setRequiredIndicatorVisible(true);
//
//        companyAddress = new TextField("Address");
//        companyAddress.setRequired(true);
//        companyAddress.setRequiredIndicatorVisible(true);
//
//        countryCode = new ComboBox<>("Country Code");
//        countryCode.setItems(countryCodes);
//        countryCode.setItemLabelGenerator(code -> code);
//        countryCode.setValue("+254"); // Default to Kenya
//        countryCode.setWidth("150px");
//
//        companyPhoneNumber = new TextField("Phone Number");
//        companyPhoneNumber.setRequired(true);
//        companyPhoneNumber.setRequiredIndicatorVisible(true);
//        companyPhoneNumber.setPlaceholder("xxxxxxxxx");
//        companyPhoneNumber.setMaxLength(9);
//        companyPhoneNumber.setMinLength(9);
//
//        // Prevent non-digit characters
//        companyPhoneNumber.addInputListener(event -> {
//            String digitsOnly = companyPhoneNumber.getValue().replaceAll("[^0-9]", "");
//            if (!companyPhoneNumber.getValue().equals(digitsOnly)) {
//                companyPhoneNumber.setValue(digitsOnly);
//            }
//        });
//
//        companyEmail = new EmailField("Company Email");
//        companyEmail.setRequired(true);
//        companyEmail.setRequiredIndicatorVisible(true);
//
//        adminFirstName = new TextField("Admin First Name");
//        adminFirstName.setRequired(true);
//        adminFirstName.setRequiredIndicatorVisible(true);
//
//        adminLastName = new TextField("Admin Last Name");
//        adminLastName.setRequired(true);
//        adminLastName.setRequiredIndicatorVisible(true);
//
//        adminUsername = new TextField("Username");
//        adminUsername.setRequired(true);
//        adminUsername.setRequiredIndicatorVisible(true);
//
//        adminEmail = new EmailField("Admin Email");
//        adminEmail.setRequired(true);
//        adminEmail.setRequiredIndicatorVisible(true);
//
//        adminPassword = new PasswordField("Password");
//        adminPassword.setRequired(true);
//        adminPassword.setRequiredIndicatorVisible(true);
//        adminPassword.setValue("Temp@123");
//        adminPassword.setHelperText("Must contain uppercase, lowercase, number, and special character");
//    }
    private void initializeFormFields() {
        companyName = new TextField("Company Name");
        companyName.setRequired(true);
        companyName.setRequiredIndicatorVisible(true);
        companyName.setErrorMessage("Please enter a valid company name");

        companyAddress = new TextField("Address");
        companyAddress.setRequired(true);
        companyAddress.setRequiredIndicatorVisible(true);

        countryCode = new ComboBox<>("Country Code");
        countryCode.setItems(countryCodes);
        countryCode.setItemLabelGenerator(code -> code);
        countryCode.setValue("+254"); // Default to Kenya
        countryCode.setWidth("150px");

        companyPhoneNumber = new TextField("Phone Number");
        companyPhoneNumber.setRequired(true);
        companyPhoneNumber.setRequiredIndicatorVisible(true);
        companyPhoneNumber.setPlaceholder("xxxxxxxxx");
        companyPhoneNumber.setMaxLength(9);
        companyPhoneNumber.setMinLength(9);

        // Prevent non-digit characters
        companyPhoneNumber.addInputListener(event -> {
            String digitsOnly = companyPhoneNumber.getValue().replaceAll("[^0-9]", "");
            if (!companyPhoneNumber.getValue().equals(digitsOnly)) {
                companyPhoneNumber.setValue(digitsOnly);
            }
        });

        companyEmail = new EmailField("Company Email");
        companyEmail.setRequired(true);
        companyEmail.setRequiredIndicatorVisible(true);
        companyEmail.setErrorMessage("Please enter a valid email address");

        adminFirstName = new TextField("Admin First Name");
        adminFirstName.setRequired(true);
        adminFirstName.setRequiredIndicatorVisible(true);

        adminLastName = new TextField("Admin Last Name");
        adminLastName.setRequired(true);
        adminLastName.setRequiredIndicatorVisible(true);

        adminUsername = new TextField("Username");
        adminUsername.setRequired(true);
        adminUsername.setRequiredIndicatorVisible(true);

        adminEmail = new EmailField("Admin Email");
        adminEmail.setRequired(true);
        adminEmail.setRequiredIndicatorVisible(true);

        adminPassword = new PasswordField("Password");
        adminPassword.setRequired(true);
        adminPassword.setRequiredIndicatorVisible(true);
        adminPassword.setValue("Temp@123");
        adminPassword.setHelperText("Must contain uppercase, lowercase, number, and special character");
        adminPassword.setErrorMessage("Password must meet the required format");
    }


//    private void setupValidation() {
//        companyBinder = new Binder<>(Company.class);
//        userBinder = new Binder<>(User.class);
//
//        // Company validation
//        companyBinder.forField(companyName)
//                .asRequired("Company name is required")
//                .withValidator(new StringLengthValidator(
//                        "Company name must be between 2 and 100 characters", 2, 100))
//                .bind(Company::getName, Company::setName);
//
//        companyBinder.forField(companyAddress)
//                .asRequired("Company address is required")
//                .withValidator(new StringLengthValidator(
//                        "Address must be between 5 and 200 characters", 5, 200))
//                .bind(Company::getAddress, Company::setAddress);
//
//        companyBinder.forField(companyEmail)
//                .asRequired("Company email is required")
//                .withValidator(new EmailValidator("Please enter a valid company email address"))
//                .bind(Company::getEmail, Company::setEmail);
//
//        // User validation
//        userBinder.forField(adminFirstName)
//                .asRequired("First name is required")
//                .withValidator(new StringLengthValidator(
//                        "First name must be between 2 and 50 characters", 2, 50))
//                .withValidator(new RegexpValidator(
//                        "First name can only contain letters", "^[a-zA-Z\\s]+$"))
//                .bind(User::getFirstName, User::setFirstName);
//
//        userBinder.forField(adminLastName)
//                .asRequired("Last name is required")
//                .withValidator(new StringLengthValidator(
//                        "Last name must be between 2 and 50 characters", 2, 50))
//                .withValidator(new RegexpValidator(
//                        "Last name can only contain letters", "^[a-zA-Z\\s]+$"))
//                .bind(User::getLastName, User::setLastName);
//
//        userBinder.forField(adminUsername)
//                .asRequired("Username is required")
//                .withValidator(new StringLengthValidator(
//                        "Username must be between 3 and 50 characters", 3, 50))
//                .withValidator(new RegexpValidator(
//                        "Username can only contain letters, numbers and underscores", "^[a-zA-Z0-9_]+$"))
//                .bind(User::getUsername, User::setUsername);
//
//        userBinder.forField(adminEmail)
//                .asRequired("Admin email is required")
//                .withValidator(new EmailValidator("Please enter a valid admin email address"))
//                .bind(User::getEmail, User::setEmail);
//
//        userBinder.forField(adminPassword)
//                .asRequired("Password is required")
//                .withValidator(new StringLengthValidator(
//                        "Password must be at least 8 characters", 8, null))
//                .withValidator(new RegexpValidator(
//                        "Password must contain uppercase, lowercase, number and special character",
//                        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"))
//                .bind(User::getPassword, User::setPassword);
//    }
    private void setupValidation() {
        companyBinder = new Binder<>(Company.class);
        userBinder = new Binder<>(User.class);

        // === Company validation ===
        companyBinder.forField(companyName)
                .asRequired("Company name is required")
                .withValidator(new StringLengthValidator(
                        "Company name must be between 2 and 100 characters", 2, 100))
                .bind(Company::getName, Company::setName);

        companyBinder.forField(companyAddress)
                .asRequired("Company address is required")
                .withValidator(new StringLengthValidator(
                        "Address must be between 5 and 200 characters", 5, 200))
                .bind(Company::getAddress, Company::setAddress);

        companyBinder.forField(companyEmail)
                .asRequired("Company email is required")
                .withValidator(new EmailValidator("Please enter a valid company email address"))
                .bind(Company::getEmail, Company::setEmail);

        // === User validation ===
        userBinder.forField(adminFirstName)
                .asRequired("First name is required")
                .withValidator(new StringLengthValidator(
                        "First name must be between 2 and 50 characters", 2, 50))
                .withValidator(new RegexpValidator(
                        "First name can only contain letters", "^[a-zA-Z\\s]+$"))
                .bind(User::getFirstName, User::setFirstName);

        userBinder.forField(adminLastName)
                .asRequired("Last name is required")
                .withValidator(new StringLengthValidator(
                        "Last name must be between 2 and 50 characters", 2, 50))
                .withValidator(new RegexpValidator(
                        "Last name can only contain letters", "^[a-zA-Z\\s]+$"))
                .bind(User::getLastName, User::setLastName);

        userBinder.forField(adminUsername)
                .asRequired("Username is required")
                .withValidator(new StringLengthValidator(
                        "Username must be between 3 and 50 characters", 3, 50))
                .withValidator(new RegexpValidator(
                        "Username can only contain letters, numbers, and underscores", "^[a-zA-Z0-9_]+$"))
                .bind(User::getUsername, User::setUsername);

        userBinder.forField(adminEmail)
                .asRequired("Admin email is required")
                .withValidator(new EmailValidator("Please enter a valid admin email address"))
                .bind(User::getEmail, User::setEmail);

        userBinder.forField(adminPassword)
                .asRequired("Password is required")
                .withValidator(new StringLengthValidator(
                        "Password must be at least 8 characters long", 8, null))
                .withValidator(new RegexpValidator(
                        "Password must contain uppercase, lowercase, number, and special character",
                        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"))
                .bind(User::getPassword, User::setPassword);

        // === Real-time validation ===
        addRealtimeValidation();
    }

    private void addRealtimeValidation() {
        // Company email real-time validation
        companyEmail.addValueChangeListener(event -> {
            companyBinder.validate();
        });

        // Admin email real-time validation
        adminEmail.addValueChangeListener(event -> {
            userBinder.validate();
        });

        // Optional: also validate as user types in password
        adminPassword.addValueChangeListener(event -> {
            userBinder.validate();
        });
    }

    private void showCompanyDialog(Company company) {
        this.currentCompany = company;
        companyDialog.removeAll();

        H1 dialogTitle = new H1(company == null ? "Register New Company" : "Edit Company");
        dialogTitle.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("font-size", "1.5rem")
                .set("margin-bottom", "1rem");

        resetForm();

        if (company != null) {
            companyName.setValue(company.getName() != null ? company.getName() : "");
            companyAddress.setValue(company.getAddress() != null ? company.getAddress() : "");

            // Parse phone number for country code and local number
            String phone = company.getPhone();
            if (phone != null && !phone.isEmpty()) {
                String countryCodeValue = "+254"; // default
                String localNumber = phone;

                for (String code : countryCodes) {
                    if (phone.startsWith(code)) {
                        countryCodeValue = code;
                        localNumber = phone.substring(code.length());
                        break;
                    }
                }

                countryCode.setValue(countryCodeValue);
                companyPhoneNumber.setValue(localNumber);
            }

            companyEmail.setValue(company.getEmail() != null ? company.getEmail() : "");

            // Hide admin fields when editing
            adminFirstName.setVisible(false);
            adminLastName.setVisible(false);
            adminUsername.setVisible(false);
            adminEmail.setVisible(false);
            adminPassword.setVisible(false);
        } else {
            // Show admin fields when creating new company
            adminFirstName.setVisible(true);
            adminLastName.setVisible(true);
            adminUsername.setVisible(true);
            adminEmail.setVisible(true);
            adminPassword.setVisible(true);
        }

        // Phone number layout
        HorizontalLayout phoneLayout = new HorizontalLayout(countryCode, companyPhoneNumber);
        phoneLayout.setWidthFull();
        phoneLayout.setFlexGrow(1, companyPhoneNumber);
        phoneLayout.setSpacing(true);

        FormLayout companyForm = new FormLayout();
        companyForm.add(companyName, companyAddress, phoneLayout, companyEmail);
        companyForm.setColspan(companyName, 1);
        companyForm.setColspan(companyAddress, 1);
        companyForm.setColspan(phoneLayout, 1);
        companyForm.setColspan(companyEmail, 1);

        companyForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        FormLayout adminForm = new FormLayout();
        adminForm.add(adminFirstName, adminLastName, adminUsername, adminEmail, adminPassword);
        adminForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        Button saveButton = new Button("Save", e -> saveCompany());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> {
            companyDialog.close();
            resetForm();
        });
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(dialogTitle, companyForm);
        if (company == null) {
            dialogLayout.add(adminForm);
        }
        dialogLayout.add(buttons);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        companyDialog.add(dialogLayout);
        companyDialog.open();
    }

    private void resetForm() {
        companyName.clear();
        companyAddress.clear();
        countryCode.setValue("+254");
        companyPhoneNumber.clear();
        companyEmail.clear();
        adminFirstName.clear();
        adminLastName.clear();
        adminUsername.clear();
        adminEmail.clear();
        adminPassword.setValue("Temp@123");

        // Clear validation errors
        companyBinder.readBean(null);
        userBinder.readBean(null);

        // Make all fields visible
        adminFirstName.setVisible(true);
        adminLastName.setVisible(true);
        adminUsername.setVisible(true);
        adminEmail.setVisible(true);
        adminPassword.setVisible(true);
    }

    private void saveCompany() {
        try {
            // Validate phone number length
            String phoneValue = companyPhoneNumber.getValue();
            if (phoneValue.length() != 9 || !phoneValue.matches("\\d{9}")) {
                Notification.show("Phone number must be exactly 9 digits", 3000, Notification.Position.MIDDLE);
                return;
            }

            // Validate full phone number
            String fullPhoneNumber = countryCode.getValue() + phoneValue;
            if (!isValidPhoneNumber(fullPhoneNumber)) {
                Notification.show("Invalid phone number format. Must be 10-13 digits including country code", 3000, Notification.Position.MIDDLE);
                return;
            }

            if (currentCompany == null) {
                // Create new company - validate all fields
                Company company = new Company();
                User adminUser = new User();

                // Validate company fields
                if (!companyBinder.writeBeanIfValid(company)) {
                    showValidationErrors();
                    return;
                }

                // Validate user fields
                if (!userBinder.writeBeanIfValid(adminUser)) {
                    showValidationErrors();
                    return;
                }

                // Set phone number with country code
                company.setPhone(fullPhoneNumber);
                company.setActive(true);

                Company savedCompany = companyService.createCompany(company);

                // Create admin user
                adminUser.setMustChangePassword(true);
                User savedAdmin = userService.createCompanyAdmin(adminUser, savedCompany);

                Notification.show("Company and admin user created successfully!", 3000, Notification.Position.MIDDLE);
            } else {
                // Update existing company
                Company company = new Company();
                if (!companyBinder.writeBeanIfValid(company)) {
                    showValidationErrors();
                    return;
                }

                currentCompany.setName(company.getName());
                currentCompany.setAddress(company.getAddress());
                currentCompany.setPhone(fullPhoneNumber);
                currentCompany.setEmail(company.getEmail());

                Company updatedCompany = companyService.updateCompany(currentCompany);
                Notification.show("Company updated successfully!", 3000, Notification.Position.MIDDLE);
            }

            companyDialog.close();
            resetForm();
            loadCompanies();
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("^\\+?[0-9]{10,13}$");
    }

    private void showValidationErrors() {
        StringBuilder errors = new StringBuilder("Please fix the following errors:\n");

        companyBinder.validate().getFieldValidationErrors().forEach(error -> {
            errors.append("• ").append(error.getMessage().orElse("Invalid value")).append("\n");
        });

        userBinder.validate().getFieldValidationErrors().forEach(error -> {
            errors.append("• ").append(error.getMessage().orElse("Invalid value")).append("\n");
        });

        Notification.show(errors.toString(), 5000, Notification.Position.MIDDLE);
    }

    private void loadCompanies() {
        companyGrid.setItems(companyService.getAllCompanies());
    }
}