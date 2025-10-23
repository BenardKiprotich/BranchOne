package com.benguides.frontend.views.expenses;

import com.benguides.dtos.ExpenseTransactionDTO;
import com.benguides.dtos.ExpenseTypeDTO;
import com.benguides.models.Branch;
import com.benguides.models.Company;
import com.benguides.models.ExpenseTransaction;
import com.benguides.models.ExpenseType;
import com.benguides.security.SecurityService;
import com.benguides.services.BranchService;
import com.benguides.services.ExpenseTransactionService;
import com.benguides.services.ExpenseTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@PageTitle("Expenses")
@Route(value = "company-admin/expenses", layout = com.benguides.frontend.layout.MainLayout.class)
@RolesAllowed({
        "ROLE_COMPANY_ADMIN",
        "ROLE_BRANCH_MANAGER",
        "ROLE_SHIFT_SUPERVISOR",
        "ROLE_SHIFT_ATTENDANT"
})
@Component
@UIScope
@RequiredArgsConstructor
public class ExpenseView extends VerticalLayout {

    private final ExpenseTransactionService expenseTransactionService;
    private final ExpenseTypeService expenseTypeService;
    private final BranchService branchService;
    private final SecurityService securityService;

    private final Tab typesTab = new Tab("Expense Types");
    private final Tab transactionsTab = new Tab("Expenses");
    private final Tabs tabs = new Tabs(typesTab, transactionsTab);

    private final VerticalLayout contentLayout = new VerticalLayout();

    private final Grid<ExpenseTypeDTO> typesGrid = new Grid<>(ExpenseTypeDTO.class, false);
    private final Grid<ExpenseTransactionDTO> transactionsGrid = new Grid<>(ExpenseTransactionDTO.class, false);

    private final boolean isCompanyAdmin;
    private Company currentCompany;
    private Branch currentBranch;
    private Long companyId;

    // For transactions pagination and filtering
    private int pageNumber = 0;
    private int pageSize = 10;
    private ComboBox<Integer> pageSizeCombo;
    private Button prevButton;
    private Button nextButton;
    private Span pageLabel;
    private DatePicker recordsFromDatePicker;
    private DatePicker recordsToDatePicker;
    private Button filterRecordsButton;

    @Autowired
    public ExpenseView(ExpenseTransactionService expenseTransactionService,
                       ExpenseTypeService expenseTypeService,
                       BranchService branchService,
                       SecurityService securityService) {
        this.expenseTransactionService = expenseTransactionService;
        this.expenseTypeService = expenseTypeService;
        this.branchService = branchService;
        this.securityService = securityService;

        this.isCompanyAdmin = securityService.hasRole("ROLE_COMPANY_ADMIN");
        this.currentCompany = securityService.getAuthenticatedUserOrThrow().getCompany();
        this.currentBranch = securityService.getAuthenticatedUserOrThrow().getBranch();
        this.companyId = currentCompany.getId();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMargin(false);

        // === PAGE BACKGROUND ===
        getStyle()
                .set("background", "linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%)")
                .set("min-height", "100vh");

        // === HEADER (Slim Version) - Matching SaleTransactionView ===
        Div headerContainer = new Div();
        headerContainer.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                .set("border-radius", "0.75rem")
                .set("padding", "1rem 1rem 0.8rem 1rem")
                .set("margin-bottom", "0.8rem")
                .set("box-shadow", "0 3px 8px rgba(0,0,0,0.1)")
                .set("color", "white")
                .set("position", "relative")
                .set("overflow", "hidden")
                .set("width", "100%")
                .set("min-height", "80px");

        Div overlay = new Div();
        overlay.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("bottom", "0")
                .set("background", "radial-gradient(circle at 30% 20%, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 70%)")
                .set("pointer-events", "none");

        H2 header = new H2("Expenses Management");
        header.getStyle()
                .set("color", "white")
                .set("font-weight", "800")
                .set("font-size", "1.8rem")
                .set("margin", "0")
                .set("text-shadow", "0 2px 4px rgba(0,0,0,0.3)")
                .set("letter-spacing", "0.5px")
                .set("position", "relative")
                .set("z-index", "1")
                .set("text-align", "center");

        VerticalLayout headerContent = new VerticalLayout(header);
        headerContent.setSpacing(false);
        headerContent.setPadding(false);
        headerContent.setAlignItems(FlexComponent.Alignment.CENTER);
        headerContent.setWidthFull();
        headerContent.getStyle()
                .set("position", "relative")
                .set("z-index", "1");

        headerContainer.add(overlay, headerContent);
        add(headerContainer);

        // === TABS ===
        configureTabs();

        tabs.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)")
                .set("padding", "0.4rem 0.8rem")
                .set("margin-bottom", "0.8rem")
                .set("display", "flex")
                .set("gap", "0.4rem");

        // Initialize tab styling
        updateTabStyles();

        tabs.addSelectedChangeListener(event -> {
            updateTabStyles();
            updateContent(event.getSelectedTab());
        });

        // === MAIN CONTENT ===
        configureTypesGrid();
        configureTransactionsGrid();

        contentLayout.setSizeFull();
        contentLayout.setPadding(true);
        contentLayout.setSpacing(true);
        contentLayout.setMargin(false);

        VerticalLayout contentCard = new VerticalLayout(contentLayout);
        contentCard.setPadding(true);
        contentCard.setSpacing(false);
        contentCard.setMargin(false);
        contentCard.setWidthFull();
        contentCard.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.07)")
                .set("border", "1px solid rgba(255,127,17,0.1)")
                .set("padding", "0.8rem 1.2rem")
                .set("margin-bottom", "0.8rem")
                .set("min-height", "250px")
                .set("max-width", "95%")
                .set("margin-left", "auto")
                .set("margin-right", "auto");

        add(tabs, contentCard);

        // Set initial content
        updateContent(typesTab);
    }

    private void updateTabStyles() {
        // Remove all existing styles first
        for (int i = 0; i < tabs.getComponentCount(); i++) {
            Tab tab = (Tab) tabs.getComponentAt(i);
            tab.getStyle()
                    .set("background", "transparent")
                    .set("color", "#0A9396")
                    .set("border-bottom", "2px solid transparent");
        }

        // Style selected tab
        Tab selectedTab = tabs.getSelectedTab();
        if (selectedTab != null) {
            selectedTab.getStyle()
                    .set("background", "rgba(255,127,17,0.15)")
                    .set("color", "#FF7F11")
                    .set("border-bottom", "2px solid #FF7F11");
        }
    }

    private void configureTabs() {
        tabs.setWidthFull();
        tabs.getStyle()
                .set("margin", "var(--lumo-space-s)")
                .set("background", "white")
                .set("border-radius", "0.5rem")
                .set("padding", "0.5rem");
    }

    private void updateContent(Tab selectedTab) {
        contentLayout.removeAll();
        if (selectedTab.equals(typesTab)) {
            contentLayout.add(createTypesSection());
        } else if (selectedTab.equals(transactionsTab)) {
            contentLayout.add(createTransactionsSection());
        }
    }

    private VerticalLayout createTypesSection() {
        VerticalLayout typesLayout = new VerticalLayout();
        typesLayout.setSpacing(true);
        typesLayout.setPadding(false);
        typesLayout.setMargin(false);
        typesLayout.setWidthFull();

        Button addTypeButton = new Button("Add Expense Type", e -> openAddTypeDialog());
        addTypeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTypeButton.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #FF9E45)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        typesLayout.add(addTypeButton, typesGrid);

        refreshTypes();

        return typesLayout;
    }

    private HorizontalLayout createDialogHeader(String title, Dialog dialog) {
    HorizontalLayout headerLayout = new HorizontalLayout();
    headerLayout.setWidthFull();
    headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    headerLayout.getStyle()
            .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
            .set("padding", "0.8rem 1.2rem")
            .set("border-radius", "0.5rem 0.5rem 0 0")
            .set("color", "white");

    H4 headerTitle = new H4(title);
    headerTitle.getStyle()
            .set("margin", "0")
            .set("color", "white")
            .set("font-weight", "800")
            .set("letter-spacing", "0.5px");

    Button closeButton = new Button("✕", e -> dialog.close());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeButton.getStyle()
            .set("color", "white")
            .set("font-weight", "700")
            .set("font-size", "1rem");

    headerLayout.add(headerTitle, closeButton);
    return headerLayout;
}

    // ---------------------------
    // ADD TYPE DIALOG
    // ---------------------------
    private void openAddTypeDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("480px");

        Div dialogContainer = new Div();
        dialogContainer.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 10px 25px rgba(0,0,0,0.15)")
                .set("border", "1px solid rgba(255,127,17,0.2)")
                .set("overflow", "hidden");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setMargin(false);
        dialogLayout.setWidthFull();
        dialogLayout.getStyle().set("padding", "1.2rem 1.5rem");

        // Styled header
        HorizontalLayout headerLayout = createDialogHeader("Add Expense Type", dialog);

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        Button saveButton = new Button("Save", e -> {
            String name = nameField.getValue();
            if (name == null || name.isBlank()) {
                showError("Name is required");
                return;
            }
            try {
                ExpenseType type = expenseTypeService.createNew(currentCompany, name);
                expenseTypeService.save(type);
                showSuccess("Expense type added");
                refreshTypes();
                dialog.close();
            } catch (Exception ex) {
                showError("Error adding type: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.getStyle().set("color", "#FF7F11");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        dialogLayout.add(headerLayout, nameField, buttonLayout);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

    // ---------------------------
    // EDIT TYPE DIALOG
    // ---------------------------
    private void openEditTypeDialog(ExpenseTypeDTO expenseTypeDTO) {
        Dialog dialog = new Dialog();
        dialog.setWidth("480px");

        Div dialogContainer = new Div();
        dialogContainer.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 10px 25px rgba(0,0,0,0.15)")
                .set("border", "1px solid rgba(255,127,17,0.2)")
                .set("overflow", "hidden");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setMargin(false);
        dialogLayout.setWidthFull();
        dialogLayout.getStyle().set("padding", "1.2rem 1.5rem");

        // Styled header
        HorizontalLayout headerLayout = createDialogHeader("Edit Expense Type", dialog);

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setValue(expenseTypeDTO.name());

        Button saveButton = new Button("Save", e -> {
            String name = nameField.getValue();
            if (name == null || name.isBlank()) {
                showError("Name is required");
                return;
            }
            try {
                Optional<ExpenseType> optionalType = expenseTypeService.findByCompanyAndName(companyId, expenseTypeDTO.name());
                if (optionalType.isPresent()) {
                    ExpenseType type = optionalType.get();
                    type.setName(name);
                    expenseTypeService.save(type);
                    showSuccess("Expense type updated");
                    refreshTypes();
                    dialog.close();
                } else {
                    showError("Expense type not found");
                }
            } catch (Exception ex) {
                showError("Error updating type: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.getStyle().set("color", "#FF7F11");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        dialogLayout.add(headerLayout, nameField, buttonLayout);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

    private void openDeleteTypeDialog(ExpenseTypeDTO expenseTypeDTO) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Confirm Delete");
        confirm.setText("Are you sure you want to delete the expense type '" + expenseTypeDTO.name() + "'?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try {
                expenseTypeService.delete(expenseTypeDTO.id());
                showSuccess("Expense type deleted");
                refreshTypes();
            } catch (Exception ex) {
                showError("Error deleting type: " + ex.getMessage());
            }
        });
        confirm.open();
    }

    private void refreshTypes() {
        List<ExpenseTypeDTO> types = expenseTypeService.findByCompanyId(companyId);
        typesGrid.setItems(types);
    }

    private VerticalLayout createTransactionsSection() {
        VerticalLayout transactionsLayout = new VerticalLayout();
        transactionsLayout.setSpacing(true);
        transactionsLayout.setPadding(false);
        transactionsLayout.setMargin(false);
        transactionsLayout.setWidthFull();

        Button addExpenseButton = new Button("Add Expenses", e -> openBatchEntryDialog());
        addExpenseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addExpenseButton.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #FF9E45)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        // Style filter section
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setAlignItems(Alignment.BASELINE);
        filterLayout.setPadding(false);
        filterLayout.setMargin(false);
        filterLayout.getStyle()
                .set("background", "rgba(255,127,17,0.05)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(255,127,17,0.1)");

        recordsFromDatePicker = new DatePicker("From Date");
        recordsFromDatePicker.setValue(LocalDate.now().minusDays(30));
        recordsToDatePicker = new DatePicker("To Date");
        recordsToDatePicker.setValue(LocalDate.now());
        filterRecordsButton = new Button("Filter", e -> {
            pageNumber = 0;
            refreshTransactions();
        });
        filterRecordsButton.getStyle()
                .set("background", "#FF7F11")
                .set("color", "white");

        filterLayout.add(recordsFromDatePicker, recordsToDatePicker, filterRecordsButton);

        // Style pagination section
        HorizontalLayout paginationLayout = new HorizontalLayout();
        paginationLayout.setAlignItems(Alignment.BASELINE);
        paginationLayout.setPadding(false);
        paginationLayout.setMargin(false);
        paginationLayout.getStyle()
                .set("background", "rgba(10,147,150,0.05)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(10,147,150,0.1)");

        pageSizeCombo = new ComboBox<>("Records per page");
        pageSizeCombo.setItems(Arrays.asList(10, 20, 50, 100));
        pageSizeCombo.setValue(10);
        pageSizeCombo.setWidth("150px");
        pageSizeCombo.addValueChangeListener(e -> {
            pageSize = e.getValue();
            pageNumber = 0;
            refreshTransactions();
        });

        prevButton = new Button("Prev", e -> {
            if (pageNumber > 0) {
                pageNumber--;
                refreshTransactions();
            }
        });
        prevButton.getStyle().set("color", "#FF7F11");

        nextButton = new Button("Next", e -> {
            pageNumber++;
            refreshTransactions();
        });
        nextButton.getStyle().set("color", "#0A9396");

        pageLabel = new Span();
        pageLabel.getStyle().set("font-weight", "600");

        paginationLayout.add(pageSizeCombo, prevButton, nextButton, pageLabel);

        transactionsLayout.add(addExpenseButton, filterLayout, paginationLayout, transactionsGrid);

        refreshTransactions();

        return transactionsLayout;
    }

    private void openBatchEntryDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");

        // Create a container for the dialog content with styling
        Div dialogContainer = new Div();
        dialogContainer.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 10px 30px rgba(0,0,0,0.2)")
                .set("border", "1px solid rgba(255,127,17,0.2)")
                .set("overflow", "hidden");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setMargin(false);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        H4 dialogHeader = new H4("Batch Expense Entry");
        dialogHeader.getStyle()
                .set("margin", "0")
                .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                .set("-webkit-background-clip", "text")
                .set("-webkit-text-fill-color", "transparent")
                .set("background-clip", "text")
                .set("font-weight", "700");

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.getStyle().set("color", "#FF7F11");

        headerLayout.add(dialogHeader, closeButton);

        // Branch, Date Selection
        HorizontalLayout selectionLayout = new HorizontalLayout();
        selectionLayout.setSpacing(true);
        selectionLayout.setAlignItems(Alignment.BASELINE);
        selectionLayout.getStyle()
                .set("background", "rgba(255,127,17,0.03)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(255,127,17,0.1)");

        DatePicker datePicker = new DatePicker("Date");
        datePicker.setValue(LocalDate.now());
        datePicker.setRequired(true);

        ComboBox<Branch> branchComboBox = null;
        TextField branchTextField = null;

        if (isCompanyAdmin) {
            branchComboBox = new ComboBox<>("Branch");
            branchComboBox.setItems(branchService.getActiveBranchesByCompany(companyId));
            branchComboBox.setItemLabelGenerator(Branch::getName);
            branchComboBox.setRequired(true);
            selectionLayout.add(branchComboBox, datePicker);
        } else {
            branchTextField = new TextField("Branch");
            branchTextField.setValue(currentBranch.getName());
            branchTextField.setReadOnly(true);
            selectionLayout.add(branchTextField, datePicker);
        }

        // Expense Entries
        FormLayout entriesForm = new FormLayout();
        entriesForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 3)
        );
        entriesForm.getStyle()
                .set("background", "rgba(10,147,150,0.03)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(10,147,150,0.1)");

        List<ExpenseType> expenseTypes = expenseTypeService.getAllByCompany(companyId);
        Map<ExpenseType, BigDecimalField> amountFields = new HashMap<>();
        Map<ExpenseType, TextField> commentFields = new HashMap<>();

        for (ExpenseType type : expenseTypes) {
            Span nameLabel = new Span(type.getName());
            nameLabel.getStyle().set("font-weight", "600").set("color", "#0A9396");

            BigDecimalField amountField = new BigDecimalField("Amount");
            amountField.setWidthFull();

            TextField commentField = new TextField("Comment");
            commentField.setWidthFull();

            entriesForm.add(nameLabel, amountField, commentField);

            amountFields.put(type, amountField);
            commentFields.put(type, commentField);
        }

        // Use final variables for use in lambda
        final ComboBox<Branch> finalBranchComboBox = branchComboBox;
        final TextField finalBranchTextField = branchTextField;

        Button saveButton = new Button("Save", e -> {
            LocalDate date = datePicker.getValue();
            if (date == null) {
                showError("Please select a date");
                return;
            }

            Branch branch;
            if (isCompanyAdmin) {
                branch = finalBranchComboBox.getValue();
                if (branch == null) {
                    showError("Please select a branch");
                    return;
                }
            } else {
                branch = currentBranch;
            }

            List<ExpenseTransaction> transactions = new ArrayList<>();
            for (ExpenseType type : expenseTypes) {
                BigDecimal amount = amountFields.get(type).getValue();
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    String comment = commentFields.get(type).getValue();
                    ExpenseTransaction trans = expenseTransactionService.createNew(currentCompany, branch, type, date);
                    trans.setAmount(amount);
                    trans.setComment(comment);
                    transactions.add(trans);
                }
            }

            if (transactions.isEmpty()) {
                showError("No expenses entered");
                return;
            }

            try {
                for (ExpenseTransaction trans : transactions) {
                    expenseTransactionService.save(trans);
                }
                showSuccess("Expenses recorded successfully");
                refreshTransactions();
                dialog.close();
            } catch (Exception ex) {
                showError("Error saving expenses: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.getStyle().set("color", "#FF7F11");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        dialogLayout.add(headerLayout, selectionLayout, entriesForm, buttonLayout);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

        private void openEditTransactionDialog(ExpenseTransactionDTO transactionDTO) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        // Create a container for the dialog content with styling
        Div dialogContainer = new Div();
        dialogContainer.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 10px 30px rgba(0,0,0,0.2)")
                .set("border", "1px solid rgba(255,127,17,0.2)")
                .set("overflow", "hidden");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setMargin(false);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        H4 dialogHeader = new H4("Edit Expense Transaction");
        dialogHeader.getStyle()
                .set("margin", "0")
                .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                .set("-webkit-background-clip", "text")
                .set("-webkit-text-fill-color", "transparent")
                .set("background-clip", "text")
                .set("font-weight", "700");

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.getStyle().set("color", "#FF7F11");

        headerLayout.add(dialogHeader, closeButton);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        formLayout.getStyle()
                .set("background", "rgba(255,127,17,0.03)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(255,127,17,0.1)");

        DatePicker datePicker = new DatePicker("Date");
        datePicker.setValue(transactionDTO.transactionDate());
        datePicker.setRequired(true);

        ComboBox<Branch> branchComboBox = null;
        TextField branchTextField = null;

        if (isCompanyAdmin) {
            branchComboBox = new ComboBox<>("Branch");
            branchComboBox.setItems(branchService.getActiveBranchesByCompany(companyId));
            branchComboBox.setItemLabelGenerator(Branch::getName);
            branchComboBox.setValue(branchService.getActiveBranchesByCompany(companyId)
                    .stream()
                    .filter(b -> b.getId().equals(transactionDTO.branchId()))
                    .findFirst()
                    .orElse(null));
            branchComboBox.setRequired(true);
            formLayout.add(branchComboBox);
        } else {
            branchTextField = new TextField("Branch");
            branchTextField.setValue(transactionDTO.branchName());
            branchTextField.setReadOnly(true);
            formLayout.add(branchTextField);
        }

        ComboBox<ExpenseType> typeComboBox = new ComboBox<>("Expense Type");
        typeComboBox.setItems(expenseTypeService.getAllByCompany(companyId));
        typeComboBox.setItemLabelGenerator(ExpenseType::getName);
        typeComboBox.setValue(expenseTypeService.getAllByCompany(companyId)
                .stream()
                .filter(t -> t.getId().equals(transactionDTO.expenseTypeId()))
                .findFirst()
                .orElse(null));
        typeComboBox.setRequired(true);

        BigDecimalField amountField = new BigDecimalField("Amount");
        amountField.setValue(transactionDTO.amount());

        TextField commentField = new TextField("Comment");
        commentField.setValue(transactionDTO.comment() != null ? transactionDTO.comment() : "");

        formLayout.add(datePicker, typeComboBox, amountField, commentField);

        // Use final variables for use in lambda
        final ComboBox<Branch> finalBranchComboBox = branchComboBox;

        Button saveButton = new Button("Save", e -> {
            LocalDate date = datePicker.getValue();
            Branch branch = isCompanyAdmin ? finalBranchComboBox.getValue() : currentBranch;
            ExpenseType type = typeComboBox.getValue();
            BigDecimal amount = amountField.getValue();

            if (date == null || (isCompanyAdmin && branch == null) || type == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Please fill all required fields with valid values");
                return;
            }

            try {
                ExpenseTransaction transaction = expenseTransactionService.createNew(currentCompany, branch, type, date);
                transaction.setId(transactionDTO.id());
                transaction.setAmount(amount);
                transaction.setComment(commentField.getValue());
                expenseTransactionService.save(transaction);
                showSuccess("Transaction updated successfully");
                refreshTransactions();
                dialog.close();
            } catch (Exception ex) {
                showError("Error updating transaction: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.getStyle().set("color", "#FF7F11");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        dialogLayout.add(headerLayout, formLayout, buttonLayout);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

    private void openDeleteTransactionDialog(ExpenseTransactionDTO transactionDTO) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Confirm Delete");
        confirm.setText("Are you sure you want to delete this transaction for " + transactionDTO.expenseTypeName() + " on " + transactionDTO.transactionDate() + "?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            try {
                expenseTransactionService.delete(transactionDTO.id());
                showSuccess("Transaction deleted successfully");
                refreshTransactions();
            } catch (Exception ex) {
                showError("Error deleting transaction: " + ex.getMessage());
            }
        });
        confirm.open();
    }

    private void refreshTransactions() {
        LocalDate startDate = recordsFromDatePicker.getValue();
        LocalDate endDate = recordsToDatePicker.getValue();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            showError("To Date cannot be before From Date. Setting To Date to From Date.");
            recordsToDatePicker.setValue(startDate);
            endDate = startDate;
        }
        Page<ExpenseTransactionDTO> page = expenseTransactionService.getAllByCompanyPaged(companyId, startDate, endDate, pageNumber, pageSize);
        transactionsGrid.setItems(page.getContent());
        pageLabel.setText("Page " + (pageNumber + 1) + " of " + page.getTotalPages());
        prevButton.setEnabled(page.hasPrevious());
        nextButton.setEnabled(page.hasNext());
    }

    private void configureTypesGrid() {
        typesGrid.addColumn(ExpenseTypeDTO::name).setHeader("Name").setWidth("200px");
        typesGrid.addColumn(ExpenseTypeDTO::createdAt).setHeader("Created At").setWidth("150px");
        typesGrid.addColumn(ExpenseTypeDTO::updatedAt).setHeader("Updated At").setWidth("150px");
        typesGrid.addComponentColumn(dto -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button editButton = new Button("Edit");
            editButton.getStyle().set("color", "#0A9396");
            editButton.addClickListener(e -> openEditTypeDialog(dto));

            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.getStyle().set("color", "#FF7F11");
            deleteButton.addClickListener(e -> openDeleteTypeDialog(dto));

            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setWidth("200px");

        typesGrid.setHeight("400px");
        typesGrid.setWidthFull();

        // Style grid to match theme
        typesGrid.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background", "white")
                .set("--lumo-primary-text-color", "#0A9396");
    }

    private void configureTransactionsGrid() {
        transactionsGrid.addColumn(ExpenseTransactionDTO::transactionDate).setHeader("Date").setWidth("120px");
        transactionsGrid.addColumn(ExpenseTransactionDTO::expenseTypeName).setHeader("Expense Type").setWidth("150px");
        transactionsGrid.addColumn(ExpenseTransactionDTO::amount).setHeader("Amount").setWidth("120px");
        transactionsGrid.addColumn(ExpenseTransactionDTO::comment).setHeader("Comment").setWidth("200px");
        transactionsGrid.addColumn(ExpenseTransactionDTO::branchName).setHeader("Branch").setWidth("150px");
        transactionsGrid.addComponentColumn(dto -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button editButton = new Button("Edit");
            editButton.getStyle().set("color", "#0A9396");
            editButton.addClickListener(e -> openEditTransactionDialog(dto));

            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.getStyle().set("color", "#FF7F11");
            deleteButton.addClickListener(e -> openDeleteTransactionDialog(dto));

            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setWidth("200px");

        transactionsGrid.setHeight("400px");
        transactionsGrid.setWidthFull();

        // Style grid to match theme
        transactionsGrid.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background", "white")
                .set("--lumo-primary-text-color", "#0A9396");
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
