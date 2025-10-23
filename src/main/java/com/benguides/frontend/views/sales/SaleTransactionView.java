package com.benguides.frontend.views.sales;

import com.benguides.dtos.SaleTransactionDTO;
import com.benguides.models.Branch;
import com.benguides.models.Company;
import com.benguides.models.Product;
import com.benguides.models.SaleTransaction;
import com.benguides.security.SecurityService;
import com.benguides.services.BranchService;
import com.benguides.services.ProductService;
import com.benguides.services.SaleTransactionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PageTitle("Sales")
@Route(value = "company-admin/sales", layout = com.benguides.frontend.layout.MainLayout.class)
@RolesAllowed({
        "ROLE_COMPANY_ADMIN",
        "ROLE_BRANCH_MANAGER",
        "ROLE_SHIFT_SUPERVISOR",
        "ROLE_SHIFT_ATTENDANT"
})
@Component
@UIScope
@RequiredArgsConstructor
public class SaleTransactionView extends VerticalLayout {

    private final SaleTransactionService saleTransactionService;
    private final ProductService productService;
    private final BranchService branchService;
    private final SecurityService securityService;

    private final Tab salesTab = new Tab("Sales");
    private final Tab analyticsTab = new Tab("Analytics");
    private final Tabs tabs = new Tabs(salesTab, analyticsTab);

    private final VerticalLayout contentLayout = new VerticalLayout();

    private final Grid<SaleTransactionDTO> salesGrid = new Grid<>(SaleTransactionDTO.class, false);
    private final boolean isCompanyAdmin;
    private Company currentCompany;
    private Branch currentBranch;
    private Long companyId;

    // Product selection instead of hardcoded products
    private ComboBox<Product> productComboBox1;
    private ComboBox<Product> productComboBox2;
    private List<Product> availableProducts;

    // Form fields
    private ComboBox<Branch> branchComboBox;
    private TextField branchTextField; // For non-admin
    private DatePicker datePicker;
    private ComboBox<SaleTransaction.ShiftSession> shiftComboBox;

    // Fields for the selected products
    private Product selectedProduct1, selectedProduct2;
    private BigDecimalField product1LitresOpen, product1LitresClose, product1CashOpen, product1CashClose, product1BuyingPrice, product1SalesLitres, product1CostOfSales;
    private BigDecimalField product2LitresOpen, product2LitresClose, product2CashOpen, product2CashClose, product2BuyingPrice, product2SalesLitres, product2CostOfSales;

    // For records pagination and filtering
    private int pageNumber = 0;
    private int pageSize = 10;
    private ComboBox<Integer> pageSizeCombo;
    private Button prevButton;
    private Button nextButton;
    private Span pageLabel;
    private DatePicker recordsFromDatePicker;
    private DatePicker recordsToDatePicker;
    private Button filterRecordsButton;

    // For analytics spans and grids
    private Span periodRevenueSpan, periodCostSpan, periodQuantitySpan, periodProfitSpan;
    private Span totalRevenueSpan, totalCostSpan, totalQuantitySpan, totalProfitSpan;
    private Span mtdRevenueSpan, mtdCostSpan, mtdQuantitySpan, mtdProfitSpan;
    private Grid<AnalyticsData> branchGrid;
    private Grid<AnalyticsData> periodBranchGrid;
    private Grid<AnalyticsData> productGrid;
    private Grid<AnalyticsData> totalProductGrid;
    private DatePicker analyticsFromDatePicker;
    private DatePicker analyticsToDatePicker;
    private Button refreshAnalyticsButton;

    // Add Sale button
    private Button addSaleButton;

    // Edit mode flags
    private boolean isEdit = false;
    private Long editId;

    // Product forms for visibility control
    private FormLayout product1Form;
    private FormLayout product2Form;

    // Current dialog reference
    private Dialog currentDialog;

    @Autowired
    public SaleTransactionView(SaleTransactionService saleTransactionService,
                               ProductService productService,
                               BranchService branchService,
                               SecurityService securityService) {
        this.saleTransactionService = saleTransactionService;
        this.productService = productService;
        this.branchService = branchService;
        this.securityService = securityService;

        this.isCompanyAdmin = securityService.hasRole("ROLE_COMPANY_ADMIN");
        this.currentCompany = securityService.getAuthenticatedUserOrThrow().getCompany();
        this.currentBranch = securityService.getAuthenticatedUserOrThrow().getBranch();
        this.companyId = currentCompany.getId();

        this.availableProducts = loadAvailableProducts();

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMargin(false);

        // === PAGE BACKGROUND ===
        getStyle()
                .set("background", "linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%)")
                .set("min-height", "100vh");

        // === HEADER (Slim Version) ===
        Div headerContainer = new Div();
        headerContainer.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                .set("border-radius", "0.75rem")
                .set("padding", "1rem 1rem 0.8rem 1rem") // Reduced bottom padding
                .set("margin-bottom", "0.8rem")
                .set("box-shadow", "0 3px 8px rgba(0,0,0,0.1)")
                .set("color", "white")
                .set("position", "relative")
                .set("overflow", "hidden")
                .set("width", "100%")
                .set("min-height", "80px"); // Reduced header height

        Div overlay = new Div();
        overlay.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("bottom", "0")
                .set("background", "radial-gradient(circle at 30% 20%, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0) 70%)")
                .set("pointer-events", "none");

        H2 header = new H2("Fuel Station Sales");
        header.getStyle()
                .set("color", "white")
                .set("font-weight", "800")
                .set("font-size", "1.8rem") // Slightly smaller
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
        configureSalesGrid();

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
                .set("padding", "0.8rem 1.2rem") // Reduced padding
                .set("margin-bottom", "0.8rem")
                .set("min-height", "250px")   // Reduced container height
                .set("max-width", "95%")
                .set("margin-left", "auto")
                .set("margin-right", "auto");

        add(tabs, contentCard);

        // Set initial content
        updateContent(salesTab);
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

    private List<Product> loadAvailableProducts() {
        try {
            return productService.getProductsByCompany(companyId, 0, 100).getContent()
                    .stream()
                    .map(dto -> {
                        Product product = new Product();
                        product.setId(dto.id());
                        product.setName(dto.name());
                        product.setUnitOfMeasurement(dto.unitOfMeasurement());
                        return product;
                    })
                    .toList();
        } catch (Exception e) {
            showError("Error loading products: " + e.getMessage());
            return List.of();
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
        if (selectedTab.equals(salesTab)) {
            contentLayout.add(createSalesSection());
        } else {
            contentLayout.add(createAnalyticsSection());
        }
    }

    private VerticalLayout createSalesSection() {
        VerticalLayout salesLayout = new VerticalLayout();
        salesLayout.setSpacing(true);
        salesLayout.setPadding(false);
        salesLayout.setMargin(false);
        salesLayout.setWidthFull();

        addSaleButton = new Button("Add Sale", e -> showSaleEntryDialog(Optional.empty()));
        addSaleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addSaleButton.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #FF9E45)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");
        salesLayout.add(addSaleButton);

        salesLayout.add(createRecordsSection());

        return salesLayout;
    }
    private void showSaleEntryDialog(Optional<SaleTransactionDTO> optDto) {
        isEdit = optDto.isPresent();
        editId = isEdit ? optDto.get().id() : null;

        currentDialog = new Dialog();
        currentDialog.setWidth("800px");
        currentDialog.setMaxWidth("90vw");
        currentDialog.setMaxHeight("90vh");

        // === Container ===
        Div dialogContainer = new Div();
        dialogContainer.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 8px 24px rgba(0,0,0,0.15)")
                .set("border", "1px solid rgba(255,127,17,0.25)")
                .set("overflow", "hidden");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setMargin(false);
        dialogLayout.setSizeFull();

        // === Header with visible gradient background ===
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setPadding(true);
        headerLayout.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                .set("color", "white")
                .set("padding", "0.75rem 1rem")
                .set("align-items", "center");

        H4 dialogHeader = new H4(isEdit ? "Edit Sale" : "Record New Sale");
        dialogHeader.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        Button closeButton = new Button("✕", e -> currentDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.getStyle()
                .set("color", "white")
                .set("font-size", "1.2em");

        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.add(dialogHeader, closeButton);

        // === Sale Entry Form ===
        VerticalLayout entryForm = createEntryForm();
        entryForm.setSizeFull();
        entryForm.getStyle().set("padding", "1rem 1.5rem");

        // === Final Assembly ===
        dialogLayout.add(headerLayout, entryForm);
        dialogContainer.add(dialogLayout);
        currentDialog.add(dialogContainer);

        // === Edit Mode Handling ===
        if (isEdit) {
            populateEditFields(optDto.get());
            productComboBox2.setVisible(false);
            product2Form.setVisible(false);
        }

        currentDialog.open();
    }

    private void populateEditFields(SaleTransactionDTO dto) {
        if (dto == null) {
            showError("No transaction to edit");
            return;
        }
        datePicker.setValue(dto.transactionDate());
        shiftComboBox.setValue(dto.shiftSession());
        if (isCompanyAdmin) {
            Optional<Branch> branchOpt = branchComboBox.getListDataView().getItems()
                    .filter(b -> b.getId().equals(dto.branchId()))
                    .findFirst();
            if (branchOpt.isPresent()) {
                branchComboBox.setValue(branchOpt.get());
            } else {
                showError("Branch not found in list");
            }
        } else {
            branchTextField.setValue(dto.branchName());
        }
        Optional<Product> productOpt = availableProducts.stream()
                .filter(p -> p.getId().equals(dto.productId()))
                .findFirst();
        if (productOpt.isPresent()) {
            productComboBox1.setValue(productOpt.get());
            selectedProduct1 = productOpt.get();
        } else {
            showError("Product not found in list");
        }
        updateProductFormVisibility();
        product1LitresOpen.setValue(dto.litresOpeningReading());
        product1LitresClose.setValue(dto.litresClosingReading());
        product1CashOpen.setValue(dto.cashOpeningReading());
        product1CashClose.setValue(dto.cashClosingReading());
        product1BuyingPrice.setValue(dto.buyingPrice());

        // Manually update calculated fields to ensure they are populated
        BigDecimal salesLitres = dto.quantity() != null ? dto.quantity() : BigDecimal.ZERO;
        product1SalesLitres.setValue(salesLitres);
        BigDecimal costOfSales = dto.costOfSales() != null ? dto.costOfSales() : BigDecimal.ZERO;
        product1CostOfSales.setValue(costOfSales);
    }

    private VerticalLayout createEntryForm() {
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(false);
        formLayout.setMargin(false);
        formLayout.setWidth("100%");

        // Header section
        H4 selectionHeader = new H4("Basic Information");
        selectionHeader.getStyle()
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");

        // Basic information in a compact two-column layout
        FormLayout basicInfoForm = new FormLayout();
        basicInfoForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );
        basicInfoForm.setWidthFull();
        basicInfoForm.getStyle()
                .set("background", "rgba(255,127,17,0.03)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(255,127,17,0.1)");

        datePicker = new DatePicker("Date");
        datePicker.setValue(LocalDate.now());

        shiftComboBox = new ComboBox<>("Shift");
        shiftComboBox.setItems(SaleTransaction.ShiftSession.values());
        shiftComboBox.setRequired(true);

        if (isCompanyAdmin) {
            branchComboBox = new ComboBox<>("Branch");
            branchComboBox.setItems(branchService.getActiveBranchesByCompany(companyId));
            branchComboBox.setItemLabelGenerator(Branch::getName);
            branchComboBox.setRequired(true);
            basicInfoForm.add(branchComboBox, datePicker, shiftComboBox);
        } else {
            branchTextField = new TextField("Branch");
            branchTextField.setValue(currentBranch.getName());
            branchTextField.setReadOnly(true);
            basicInfoForm.add(branchTextField, datePicker, shiftComboBox);
        }

        // Product selection in a compact two-column layout
        H4 productHeader = new H4("Product Selection");
        productHeader.getStyle()
                .set("margin", "var(--lumo-space-m) 0 var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");

        FormLayout productSelectionForm = new FormLayout();
        productSelectionForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );
        productSelectionForm.setWidthFull();
        productSelectionForm.getStyle()
                .set("background", "rgba(10,147,150,0.03)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(10,147,150,0.1)");

        productComboBox1 = new ComboBox<>("Product 1");
        productComboBox1.setItems(availableProducts);
        productComboBox1.setItemLabelGenerator(Product::getName);
        productComboBox1.addValueChangeListener(e -> {
            selectedProduct1 = e.getValue();
            updateProductFormVisibility();
        });

        productComboBox2 = new ComboBox<>("Product 2");
        productComboBox2.setItems(availableProducts);
        productComboBox2.setItemLabelGenerator(Product::getName);
        productComboBox2.addValueChangeListener(e -> {
            selectedProduct2 = e.getValue();
            updateProductFormVisibility();
        });

        productSelectionForm.add(productComboBox1, productComboBox2);

        // Product details in a compact two-column layout
        HorizontalLayout productDetailsLayout = new HorizontalLayout();
        productDetailsLayout.setSpacing(true);
        productDetailsLayout.setWidthFull();
        productDetailsLayout.setPadding(false);
        productDetailsLayout.setMargin(false);

        // orange/teal theme
        product1Form = createCompactProductForm("Product 1", "rgba(255,127,17,0.08)", true);
        product2Form = createCompactProductForm("Product 2", "rgba(10,147,150,0.08)", false);

        product1Form.setWidth("48%");
        product2Form.setWidth("48%");

        productDetailsLayout.add(product1Form, product2Form);

        // Save button
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button saveButton = new Button("Save Sales", e -> saveSales());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");
        buttonLayout.add(saveButton);

        // Add all components to main form layout
        formLayout.add(selectionHeader, basicInfoForm, productHeader, productSelectionForm, productDetailsLayout, buttonLayout);

        updateProductFormVisibility();

        return formLayout;
    }

    private void updateProductFormVisibility() {
        if (product1Form != null) {
            product1Form.setVisible(selectedProduct1 != null);
        }
        if (product2Form != null) {
            product2Form.setVisible(selectedProduct2 != null);
        }
    }

    private FormLayout createCompactProductForm(String productLabel, String color, boolean isProduct1) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("250px", 2)
        );
        form.getStyle()
                .set("background", color)
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin", "var(--lumo-space-xs)")
                .set("border", isProduct1 ? "1px solid rgba(255,127,17,0.2)" : "1px solid rgba(10,147,150,0.2)");

        // Header
        H4 productHeader = new H4(productLabel);
        productHeader.getStyle()
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", isProduct1 ? "#FF7F11" : "#0A9396")
                .set("font-weight", "600");
        form.add(productHeader, 2);

        // Cash section - more compact
        Span cashLabel = new Span("Cash Readings");
        cashLabel.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "var(--lumo-space-xs) 0")
                .set("color", isProduct1 ? "#B35C00" : "#06646D");
        form.add(cashLabel, 2);

        BigDecimalField openCash = new BigDecimalField("Opening");
        openCash.setWidth("100%");
        openCash.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        BigDecimalField closeCash = new BigDecimalField("Closing");
        closeCash.setWidth("100%");
        closeCash.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        BigDecimalField salesCash = new BigDecimalField("Sales");
        salesCash.setWidth("100%");
        salesCash.setReadOnly(true);
        salesCash.setValue(BigDecimal.ZERO);
        salesCash.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        closeCash.addValueChangeListener(e -> {
            if (openCash.getValue() != null && closeCash.getValue() != null) {
                salesCash.setValue(closeCash.getValue().subtract(openCash.getValue()));
            }
        });

        form.add(openCash, closeCash);
        form.add(salesCash, 2);

        // Litres section
        Span litresLabel = new Span("Litres Readings");
        litresLabel.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "var(--lumo-space-xs) 0")
                .set("color", isProduct1 ? "#B35C00" : "#06646D");
        form.add(litresLabel, 2);

        BigDecimalField openLitres = new BigDecimalField("Opening");
        openLitres.setWidth("100%");
        openLitres.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        BigDecimalField closeLitres = new BigDecimalField("Closing");
        closeLitres.setWidth("100%");
        closeLitres.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        BigDecimalField salesLitres = new BigDecimalField("Sales");
        salesLitres.setWidth("100%");
        salesLitres.setReadOnly(true);
        salesLitres.setValue(BigDecimal.ZERO);
        salesLitres.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        BigDecimalField buyingPrice = new BigDecimalField("Buying Price");
        buyingPrice.setWidth("100%");
        buyingPrice.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        BigDecimalField costOfSales = new BigDecimalField("Cost of Sales");
        costOfSales.setWidth("100%");
        costOfSales.setReadOnly(true);
        costOfSales.getStyle().set("font-size", "var(--lumo-font-size-xs)");

        closeLitres.addValueChangeListener(e -> {
            if (openLitres.getValue() != null && closeLitres.getValue() != null) {
                salesLitres.setValue(openLitres.getValue().subtract(closeLitres.getValue()).abs());
            }
            updateProductCost(isProduct1);
        });

        buyingPrice.addValueChangeListener(e -> updateProductCost(isProduct1));

        form.add(openLitres, closeLitres);
        form.add(salesLitres, 2);
        form.add(buyingPrice, costOfSales);

        if (isProduct1) {
            product1CashOpen = openCash;
            product1CashClose = closeCash;
            product1LitresOpen = openLitres;
            product1LitresClose = closeLitres;
            product1BuyingPrice = buyingPrice;
            product1SalesLitres = salesLitres;
            product1CostOfSales = costOfSales;
        } else {
            product2CashOpen = openCash;
            product2CashClose = closeCash;
            product2LitresOpen = openLitres;
            product2LitresClose = closeLitres;
            product2BuyingPrice = buyingPrice;
            product2SalesLitres = salesLitres;
            product2CostOfSales = costOfSales;
        }

        return form;
    }

    private void updateProductCost(boolean isProduct1) {
        if (isProduct1) {
            BigDecimal litres = (product1LitresOpen.getValue() != null && product1LitresClose.getValue() != null) ?
                    product1LitresOpen.getValue().subtract(product1LitresClose.getValue()).abs() : BigDecimal.ZERO;
            product1SalesLitres.setValue(litres);
            if (product1BuyingPrice.getValue() != null) {
                product1CostOfSales.setValue(litres.multiply(product1BuyingPrice.getValue()));
            }
        } else {
            BigDecimal litres = (product2LitresOpen.getValue() != null && product2LitresClose.getValue() != null) ?
                    product2LitresOpen.getValue().subtract(product2LitresClose.getValue()).abs() : BigDecimal.ZERO;
            product2SalesLitres.setValue(litres);
            if (product2BuyingPrice.getValue() != null) {
                product2CostOfSales.setValue(litres.multiply(product2BuyingPrice.getValue()));
            }
        }
    }

    private void saveSales() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showError("Please select a date");
            return;
        }

        SaleTransaction.ShiftSession shift = shiftComboBox.getValue();
        if (shift == null) {
            showError("Please select a shift");
            return;
        }

        Branch branch = isCompanyAdmin ? branchComboBox.getValue() : currentBranch;
        if (branch == null) {
            showError("Please select a branch");
            return;
        }

        try {
            if (isEdit) {
                if (selectedProduct1 == null) {
                    showError("Please select Product 1");
                    return;
                }
                if (product1BuyingPrice.getValue() == null) {
                    showError("Please enter buying price for " + selectedProduct1.getName());
                    return;
                }
                Optional<SaleTransaction> optTrans = saleTransactionService.findById(editId);
                if (optTrans.isEmpty()) {
                    showError("Sale not found for update");
                    return;
                }
                SaleTransaction trans = optTrans.get();
                trans.setTransactionDate(date);
                trans.setShiftSession(shift);
                trans.setBranch(branch);
                trans.setProduct(selectedProduct1);
                trans.setLitresOpeningReading(product1LitresOpen.getValue());
                trans.setLitresClosingReading(product1LitresClose.getValue());
                trans.setCashOpeningReading(product1CashOpen.getValue());
                trans.setCashClosingReading(product1CashClose.getValue());
                trans.setBuyingPrice(product1BuyingPrice.getValue());
                saleTransactionService.save(trans);
                showSuccess("Sale updated successfully");
            } else {
                if (selectedProduct1 == null && selectedProduct2 == null) {
                    showError("Please select at least one product");
                    return;
                }

                if (selectedProduct1 != null) {
                    if (product1BuyingPrice.getValue() == null) {
                        showError("Please enter buying price for " + selectedProduct1.getName());
                        return;
                    }
                    SaleTransaction trans1 = saleTransactionService.createNew(currentCompany, branch, selectedProduct1, shift, date);
                    trans1.setLitresOpeningReading(product1LitresOpen.getValue());
                    trans1.setLitresClosingReading(product1LitresClose.getValue());
                    trans1.setCashOpeningReading(product1CashOpen.getValue());
                    trans1.setCashClosingReading(product1CashClose.getValue());
                    trans1.setBuyingPrice(product1BuyingPrice.getValue());
                    saleTransactionService.save(trans1);
                }

                if (selectedProduct2 != null) {
                    if (product2BuyingPrice.getValue() == null) {
                        showError("Please enter buying price for " + selectedProduct2.getName());
                        return;
                    }
                    SaleTransaction trans2 = saleTransactionService.createNew(currentCompany, branch, selectedProduct2, shift, date);
                    trans2.setLitresOpeningReading(product2LitresOpen.getValue());
                    trans2.setLitresClosingReading(product2LitresClose.getValue());
                    trans2.setCashOpeningReading(product2CashOpen.getValue());
                    trans2.setCashClosingReading(product2CashClose.getValue());
                    trans2.setBuyingPrice(product2BuyingPrice.getValue());
                    saleTransactionService.save(trans2);
                }
                showSuccess("Sales entries recorded successfully");
            }
            clearForm();
            refreshRecords();
            if (currentDialog != null) {
                currentDialog.close();
            }
        } catch (Exception ex) {
            showError("Error saving sales: " + ex.getMessage());
        }
    }

    private void clearForm() {
        List<BigDecimalField> fields = List.of(
                product1LitresOpen, product1LitresClose, product1CashOpen, product1CashClose, product1BuyingPrice, product1SalesLitres, product1CostOfSales,
                product2LitresOpen, product2LitresClose, product2CashOpen, product2CashClose, product2BuyingPrice, product2SalesLitres, product2CostOfSales
        );
        fields.forEach(f -> {
            if (f != null) f.clear();
        });
        if (isCompanyAdmin && branchComboBox != null) branchComboBox.clear();
        if (shiftComboBox != null) shiftComboBox.clear();
        if (productComboBox1 != null) productComboBox1.clear();
        if (productComboBox2 != null) productComboBox2.clear();
        selectedProduct1 = null;
        selectedProduct2 = null;
        if (datePicker != null) datePicker.setValue(LocalDate.now());
        updateProductFormVisibility();
        productComboBox2.setVisible(true);
        product2Form.setVisible(selectedProduct2 != null);
    }

    private VerticalLayout createRecordsSection() {
        VerticalLayout recordsLayout = new VerticalLayout();
        recordsLayout.setSpacing(true);
        recordsLayout.setPadding(false);
        recordsLayout.setMargin(false);
        recordsLayout.setWidthFull();

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
        recordsFromDatePicker.setValue(LocalDate.now().minusDays(7));
        recordsToDatePicker = new DatePicker("To Date");
        recordsToDatePicker.setValue(LocalDate.now());
        filterRecordsButton = new Button("Filter", e -> {
            pageNumber = 0;
            refreshRecords();
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
            refreshRecords();
        });

        prevButton = new Button("Prev", e -> {
            if (pageNumber > 0) {
                pageNumber--;
                refreshRecords();
            }
        });
        prevButton.getStyle().set("color", "#FF7F11");

        nextButton = new Button("Next", e -> {
            pageNumber++;
            refreshRecords();
        });
        nextButton.getStyle().set("color", "#0A9396");

        pageLabel = new Span();
        pageLabel.getStyle().set("font-weight", "600");

        paginationLayout.add(pageSizeCombo, prevButton, nextButton, pageLabel);

        recordsLayout.add(filterLayout, paginationLayout, salesGrid);

        refreshRecords();

        return recordsLayout;
    }

    private void refreshRecords() {
        try {
            LocalDate startDate = recordsFromDatePicker.getValue();
            LocalDate endDate = recordsToDatePicker.getValue();
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                showError("To Date cannot be before From Date. Setting To Date to From Date.");
                recordsToDatePicker.setValue(startDate);
                endDate = startDate;
            }
            Page<SaleTransactionDTO> page = saleTransactionService.getAllByCompanyPaged(companyId, startDate, endDate, pageNumber, pageSize);
            salesGrid.setItems(page.getContent());
            pageLabel.setText("Page " + (pageNumber + 1) + " of " + page.getTotalPages());
            prevButton.setEnabled(page.hasPrevious());
            nextButton.setEnabled(page.hasNext());
        } catch (Exception e) {
            showError("Error loading records: " + e.getMessage());
        }
    }

    private void configureSalesGrid() {
        salesGrid.addColumn(SaleTransactionDTO::transactionDate).setHeader("Date").setWidth("120px");
        salesGrid.addColumn(SaleTransactionDTO::productName).setHeader("Product").setWidth("100px");
        salesGrid.addColumn(SaleTransactionDTO::shiftSession).setHeader("Shift").setWidth("100px");
        salesGrid.addColumn(SaleTransactionDTO::branchName).setHeader("Branch").setWidth("150px");
        salesGrid.addColumn(SaleTransactionDTO::quantity).setHeader("Quantity (Litres)").setWidth("100px");
        salesGrid.addColumn(SaleTransactionDTO::totalAmount).setHeader("Total Amount").setWidth("120px");
        salesGrid.addColumn(SaleTransactionDTO::costOfSales).setHeader("Cost of Sales").setWidth("120px");
        salesGrid.addComponentColumn(dto -> {
            HorizontalLayout layout = new HorizontalLayout();
            Button editButton = new Button("Edit");
            editButton.getStyle().set("color", "#0A9396");
            editButton.addClickListener(e -> editSale(dto));
            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.getStyle().set("color", "#FF7F11");
            deleteButton.addClickListener(e -> confirmDelete(dto));
            layout.add(editButton, deleteButton);
            return layout;
        }).setHeader("Actions").setWidth("150px");

        salesGrid.setHeight("400px");
        salesGrid.setWidthFull();

        // Style grid to match CompanyAdminView
        salesGrid.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background", "white")
                .set("--lumo-primary-text-color", "#0A9396");
    }

    private void editSale(SaleTransactionDTO dto) {
        showSaleEntryDialog(Optional.of(dto));
    }

    private void confirmDelete(SaleTransactionDTO dto) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Confirm Delete");
        confirm.setText("Are you sure you want to delete this sale?");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            saleTransactionService.delete(dto.id());
            refreshRecords();
            showSuccess("Sale deleted successfully");
        });
        confirm.open();
    }

    private VerticalLayout createAnalyticsSection() {
        VerticalLayout analyticsLayout = new VerticalLayout();
        analyticsLayout.setSpacing(true);
        analyticsLayout.setPadding(false);
        analyticsLayout.setMargin(false);
        analyticsLayout.setWidthFull();

        // Style period selection
        HorizontalLayout periodSelectionLayout = new HorizontalLayout();
        periodSelectionLayout.setAlignItems(Alignment.BASELINE);
        periodSelectionLayout.getStyle()
                .set("background", "rgba(255,127,17,0.05)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "0.5rem")
                .set("border", "1px solid rgba(255,127,17,0.1)");

        analyticsFromDatePicker = new DatePicker("From Date");
        analyticsFromDatePicker.setValue(LocalDate.now().minusDays(7));
        analyticsToDatePicker = new DatePicker("To Date");
        analyticsToDatePicker.setValue(LocalDate.now());
        refreshAnalyticsButton = new Button("Refresh Period Analytics", e -> refreshPeriodAnalytics(analyticsFromDatePicker.getValue(), analyticsToDatePicker.getValue()));
        refreshAnalyticsButton.getStyle()
                .set("background", "#FF7F11")
                .set("color", "white");

        periodSelectionLayout.add(analyticsFromDatePicker, analyticsToDatePicker, refreshAnalyticsButton);
        analyticsLayout.add(periodSelectionLayout);

        // Update summary sections to match your color scheme
        H4 periodCompanySummaryHeader = new H4("Period Company Sales Summary");
        periodCompanySummaryHeader.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");

        VerticalLayout periodCompanySummary = new VerticalLayout();
        periodCompanySummary.getStyle()
                .set("background", "linear-gradient(135deg, #FFEDD8, #FFF5EB)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("border", "1px solid rgba(255,127,17,0.2)");
        periodCompanySummary.setSpacing(false);
        periodCompanySummary.add(createSummaryRow("Total Revenue: ", periodRevenueSpan = new Span("0")));
        periodCompanySummary.add(createSummaryRow("Total Cost: ", periodCostSpan = new Span("0")));
        periodCompanySummary.add(createSummaryRow("Total Quantity (Litres): ", periodQuantitySpan = new Span("0")));
        periodCompanySummary.add(createSummaryRow("Profit: ", periodProfitSpan = new Span("0")));
        analyticsLayout.add(periodCompanySummaryHeader, periodCompanySummary);

        H4 periodBranchSalesHeader = new H4("Period Sales Per Branch");
        periodBranchSalesHeader.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");
        periodBranchGrid = createAnalyticsGrid();
        analyticsLayout.add(periodBranchSalesHeader, periodBranchGrid);

        H4 periodProductSalesHeader = new H4("Period Sales Per Product");
        periodProductSalesHeader.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");
        productGrid = createAnalyticsGrid();
        analyticsLayout.add(periodProductSalesHeader, productGrid);

        H4 mtdSummaryHeader = new H4("Month to Date Sales Summary");
        mtdSummaryHeader.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");

        VerticalLayout mtdSummary = new VerticalLayout();
        mtdSummary.getStyle()
                .set("background", "linear-gradient(135deg, #E3F2FD, #E8F4FD)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("border", "1px solid rgba(10,147,150,0.2)");
        mtdSummary.setSpacing(false);
        mtdSummary.add(createSummaryRow("Total Revenue: ", mtdRevenueSpan = new Span("0")));
        mtdSummary.add(createSummaryRow("Total Cost: ", mtdCostSpan = new Span("0")));
        mtdSummary.add(createSummaryRow("Total Quantity (Litres): ", mtdQuantitySpan = new Span("0")));
        mtdSummary.add(createSummaryRow("Profit: ", mtdProfitSpan = new Span("0")));
        analyticsLayout.add(mtdSummaryHeader, mtdSummary);

        H4 branchSalesHeader = new H4("Sales Per Branch (All Time)");
        branchSalesHeader.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");
        branchGrid = createAnalyticsGrid();
        analyticsLayout.add(branchSalesHeader, branchGrid);

        H4 totalProductSalesHeader = new H4("Total Sales Per Product (All Time)");
        totalProductSalesHeader.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");
        totalProductGrid = createAnalyticsGrid();
        analyticsLayout.add(totalProductSalesHeader, totalProductGrid);

        H4 totalCompanySummaryHeader = new H4("Company Total Sales (All Time)");
        totalCompanySummaryHeader.getStyle()
                .set("margin", "var(--lumo-space-s) 0")
                .set("color", "#0A9396")
                .set("font-weight", "600");

        VerticalLayout totalCompanySummary = new VerticalLayout();
        totalCompanySummary.getStyle()
                .set("background", "linear-gradient(135deg, #F3E5F5, #F8EAF9)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("border", "1px solid rgba(139,69,19,0.2)");
        totalCompanySummary.setSpacing(false);
        totalCompanySummary.add(createSummaryRow("Total Revenue: ", totalRevenueSpan = new Span("0")));
        totalCompanySummary.add(createSummaryRow("Total Cost: ", totalCostSpan = new Span("0")));
        totalCompanySummary.add(createSummaryRow("Total Quantity (Litres): ", totalQuantitySpan = new Span("0")));
        totalCompanySummary.add(createSummaryRow("Profit: ", totalProfitSpan = new Span("0")));
        analyticsLayout.add(totalCompanySummaryHeader, totalCompanySummary);

        refreshAnalytics();

        return analyticsLayout;
    }

    private void refreshAnalytics() {
        refreshPeriodAnalytics(LocalDate.now().minusDays(7), LocalDate.now());

        try {
            BigDecimal[] mtdSummary = saleTransactionService.getMonthToDateCompanySalesSummary(companyId);
            periodAnalytics(mtdSummary, mtdRevenueSpan, mtdCostSpan, mtdQuantitySpan, mtdProfitSpan);

            Map<String, BigDecimal[]> branchData = saleTransactionService.getSalesPerBranch(companyId);
            List<AnalyticsData> branchList = convertToAnalyticsData(branchData);
            branchGrid.setItems(branchList);

            Map<String, BigDecimal[]> totalProductData = saleTransactionService.getSalesPerProduct(companyId);
            List<AnalyticsData> totalProductList = convertToAnalyticsData(totalProductData);
            totalProductGrid.setItems(totalProductList);

            BigDecimal[] totalSummary = saleTransactionService.getCompanyTotalSalesSummary(companyId);
            periodAnalytics(totalSummary, totalRevenueSpan, totalCostSpan, totalQuantitySpan, totalProfitSpan);
        } catch (Exception e) {
            showError("Error loading analytics: " + e.getMessage());
        }
    }

    private void refreshPeriodAnalytics(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            showError("Please select both From and To dates");
            return;
        }
        if (to.isBefore(from)) {
            showError("To Date cannot be before From Date. Setting To Date to From Date.");
            analyticsToDatePicker.setValue(from);
            to = from;
        }
        try {
            BigDecimal[] periodSummary = saleTransactionService.getSalesSummaryBetween(companyId, from, to);
            periodAnalytics(periodSummary, periodRevenueSpan, periodCostSpan, periodQuantitySpan, periodProfitSpan);

            Map<String, BigDecimal[]> periodBranchData = saleTransactionService.getSalesPerBranchBetween(companyId, from, to);
            List<AnalyticsData> periodBranchList = convertToAnalyticsData(periodBranchData);
            periodBranchGrid.setItems(periodBranchList);

            Map<String, BigDecimal[]> periodProductData = saleTransactionService.getSalesPerProductBetween(companyId, from, to);
            List<AnalyticsData> periodProductList = convertToAnalyticsData(periodProductData);
            productGrid.setItems(periodProductList);

        } catch (Exception e) {
            showError("Error loading period analytics: " + e.getMessage());
        }
    }

    private void periodAnalytics(BigDecimal[] periodSummary, Span periodRevenueSpan, Span periodCostSpan, Span periodQuantitySpan, Span periodProfitSpan) {
        BigDecimal periodRevenue = periodSummary != null && periodSummary.length > 0 ? periodSummary[0] : BigDecimal.ZERO;
        BigDecimal periodCost = periodSummary != null && periodSummary.length > 1 ? periodSummary[1] : BigDecimal.ZERO;
        BigDecimal periodQuantity = periodSummary != null && periodSummary.length > 2 ? periodSummary[2] : BigDecimal.ZERO;
        BigDecimal periodProfit = periodRevenue.subtract(periodCost);
        periodRevenueSpan.setText(formatCurrency(periodRevenue));
        periodCostSpan.setText(formatCurrency(periodCost));
        periodQuantitySpan.setText(formatNumber(periodQuantity));
        periodProfitSpan.setText(formatCurrency(periodProfit));
    }

    private Grid<AnalyticsData> createAnalyticsGrid() {
        Grid<AnalyticsData> grid = new Grid<>();
        grid.addColumn(AnalyticsData::getName).setHeader("Name").setWidth("150px");
        grid.addColumn(data -> formatCurrency(data.getRevenue())).setHeader("Revenue").setWidth("120px");
        grid.addColumn(data -> formatCurrency(data.getCost())).setHeader("Cost").setWidth("120px");
        grid.addColumn(data -> formatNumber(data.getQuantity())).setHeader("Quantity (Litres)").setWidth("140px");
        grid.addColumn(data -> formatCurrency(data.getProfit())).setHeader("Profit").setWidth("120px");
        grid.setHeight("200px");
        grid.setWidthFull();

        // Style analytics grids to match theme
        grid.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background", "white")
                .set("--lumo-primary-text-color", "#0A9396");

        return grid;
    }

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString() : "0.00";
    }

    private String formatNumber(BigDecimal number) {
        return number != null ? number.setScale(2, BigDecimal.ROUND_HALF_UP) + " L" : "0.00 L";
    }

    private List<AnalyticsData> convertToAnalyticsData(Map<String, BigDecimal[]> data) {
        return data.entrySet().stream()
                .map(entry -> {
                    BigDecimal[] values = entry.getValue();
                    BigDecimal revenue = values != null && values.length > 0 ? values[0] : BigDecimal.ZERO;
                    BigDecimal cost = values != null && values.length > 1 ? values[1] : BigDecimal.ZERO;
                    BigDecimal quantity = values != null && values.length > 2 ? values[2] : BigDecimal.ZERO;
                    BigDecimal profit = revenue.subtract(cost);
                    return new AnalyticsData(entry.getKey(), revenue, cost, quantity, profit);
                })
                .toList();
    }

    private HorizontalLayout createSummaryRow(String label, Span valueSpan) {
        HorizontalLayout row = new HorizontalLayout(new Span(label), valueSpan);
        row.setWidthFull();
        row.setPadding(false);
        row.setMargin(false);
        row.setAlignItems(Alignment.BASELINE);
        row.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        // Style the value spans
        valueSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#0A9396")
                .set("margin-left", "auto");

        return row;
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private static class AnalyticsData {
        private final String name;
        private final BigDecimal revenue;
        private final BigDecimal cost;
        private final BigDecimal quantity;
        private final BigDecimal profit;

        public AnalyticsData(String name, BigDecimal revenue, BigDecimal cost, BigDecimal quantity, BigDecimal profit) {
            this.name = name;
            this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
            this.cost = cost != null ? cost : BigDecimal.ZERO;
            this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
            this.profit = profit != null ? profit : BigDecimal.ZERO;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public BigDecimal getProfit() {
            return profit;
        }
    }
}
