package com.benguides.frontend.views.inventory;

import com.benguides.dtos.ProductCategoryDTO;
import com.benguides.dtos.ProductDTO;
import com.benguides.models.Product;
import com.benguides.models.ProductCategory;
import com.benguides.security.SecurityService;
import com.benguides.services.CompanyService;
import com.benguides.services.ProductCategoryService;
import com.benguides.services.ProductService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@PageTitle("Inventory")
@Route(value = "inventory", layout = com.benguides.frontend.layout.MainLayout.class)
@RolesAllowed({
        "ROLE_COMPANY_ADMIN",
        "ROLE_BRANCH_MANAGER",
        "ROLE_SHIFT_SUPERVISOR",
        "ROLE_SHIFT_ATTENDANT"
})
@Component
@UIScope
@RequiredArgsConstructor
public class ProductCategoryAndProductView extends VerticalLayout {

    private final ProductService productService;
    private final ProductCategoryService categoryService;
    private final CompanyService companyService;
    private final SecurityService securityService;

    private final Grid<ProductDTO> productGrid = new Grid<>(ProductDTO.class, false);
    private final Grid<ProductCategoryDTO> categoryGrid = new Grid<>(ProductCategoryDTO.class, false);

    // Changed order: Categories first, then Products
    private final Tab categoriesTab = new Tab("Categories");
    private final Tab productsTab = new Tab("Products");
    private final Tabs tabs = new Tabs(categoriesTab, productsTab); // Categories on left, Products on right

    private final VerticalLayout contentLayout = new VerticalLayout();
    private final HorizontalLayout actionButtonsLayout = new HorizontalLayout();
    private final boolean isAdmin;
    private Button addProductButton;
    private Button addCategoryButton;

    private int productPage = 0;
    private int categoryPage = 0;
    private static final int PAGE_SIZE = 20;

    @Autowired
    public ProductCategoryAndProductView(ProductService productService,
                                         ProductCategoryService categoryService,
                                         CompanyService companyService,
                                         SecurityService securityService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.companyService = companyService;
        this.securityService = securityService;
        this.isAdmin = securityService.hasRole("ROLE_COMPANY_ADMIN");

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMargin(false);

        // === PAGE BACKGROUND ===
        getStyle()
                .set("background", "linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%)")
                .set("min-height", "100vh");

        // === HEADER (Slim Version) - Matching Other Views ===
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

        H2 header = new H2("Inventory Management");
        header.getStyle()
                .set("color", "white") // White font color as requested
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
        configureActionButtonsLayout();
        configureGrids();

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
        updateContent(categoriesTab);
        tabs.setSelectedTab(categoriesTab);
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Start with Categories tab selected (leftmost tab)
        updateContent(categoriesTab);
        tabs.setSelectedTab(categoriesTab);
    }

    private void configureActionButtonsLayout() {
        actionButtonsLayout.setWidthFull();
        actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        actionButtonsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        actionButtonsLayout.setSpacing(true);
        actionButtonsLayout.getStyle()
                .set("margin-bottom", "1rem")
                .set("padding", "0");

        if (isAdmin) {
            addCategoryButton = new Button("Add Category", e -> openCategoryDialog(null));
            addCategoryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addCategoryButton.getStyle()
                    .set("background", "linear-gradient(135deg, #FF7F11, #FF9E45)")
                    .set("color", "white")
                    .set("font-weight", "600")
                    .set("border", "none")
                    .set("margin-right", "auto");

            addProductButton = new Button("Add Product", e -> openProductDialog(null));
            addProductButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addProductButton.getStyle()
                    .set("background", "linear-gradient(135deg, #FF7F11, #FF9E45)")
                    .set("color", "white")
                    .set("font-weight", "600")
                    .set("border", "none")
                    .set("margin-left", "0");

            actionButtonsLayout.add(addCategoryButton, addProductButton);
            contentLayout.add(actionButtonsLayout);
        }
    }

    private void configureTabs() {
        tabs.getStyle()
                .set("margin", "var(--lumo-space-s)")
                .set("background", "white")
                .set("border-radius", "0.5rem")
                .set("padding", "0.5rem");
    }

    private void updateContent(Tab selectedTab) {
        contentLayout.removeAll();
        if (isAdmin) {
            contentLayout.add(actionButtonsLayout);
        }

        if (selectedTab.equals(productsTab)) {
            refreshProducts();
            contentLayout.add(productGrid);
            // Show/hide appropriate buttons
            if (isAdmin) {
                addProductButton.setVisible(true);
                addCategoryButton.setVisible(false);
            }
        } else {
            // Categories tab is selected (this is now the default/first tab)
            refreshCategories();
            contentLayout.add(categoryGrid);
            // Show/hide appropriate buttons
            if (isAdmin) {
                addProductButton.setVisible(false);
                addCategoryButton.setVisible(true);
            }
        }
    }

    private void configureGrids() {
        configureProductGrid();
        configureCategoryGrid();
    }

    private void configureProductGrid() {
        // Style the grid to match theme
        productGrid.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background", "white")
                .set("--lumo-primary-text-color", "#0A9396");

        productGrid.addColumn(ProductDTO::id).setHeader("ID").setAutoWidth(true);
        productGrid.addColumn(ProductDTO::name).setHeader("Product Name").setAutoWidth(true);
        productGrid.addColumn(ProductDTO::categoryName).setHeader("Category").setAutoWidth(true);
        productGrid.addColumn(ProductDTO::unitOfMeasurement).setHeader("Unit").setAutoWidth(true);
        productGrid.addColumn(dto -> dto.active() ? "Yes" : "No").setHeader("Active").setAutoWidth(true);

        if (isAdmin) {
            productGrid.addComponentColumn(dto -> {
                HorizontalLayout actions = new HorizontalLayout();
                actions.setSpacing(true);

                Button editBtn = new Button("Edit");
                editBtn.getStyle().set("color", "#0A9396");
                editBtn.addClickListener(e -> openProductDialog(dto));

                Button deleteBtn = new Button("Delete");
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                deleteBtn.getStyle().set("color", "#FF7F11");
                deleteBtn.addClickListener(e -> {
                    productService.delete(dto.id());
                    refreshProducts();
                });

                actions.add(editBtn, deleteBtn);
                return actions;
            }).setHeader("Actions").setAutoWidth(true);
        } else {
            productGrid.addComponentColumn(dto -> {
                Button viewBtn = new Button("View Details");
                viewBtn.getStyle().set("color", "#0A9396");
                viewBtn.addClickListener(e -> openViewProductDialog(dto));
                return viewBtn;
            }).setHeader("Action").setAutoWidth(true);
        }

        // ✅ Lazy pagination
        productGrid.setItems(query -> {
            int page = query.getPage();
            Long companyId = securityService.getAuthenticatedUser()
                    .map(u -> u.getCompany().getId())
                    .orElseThrow();
            Page<ProductDTO> products = productService.getProductsByCompany(companyId, page, PAGE_SIZE);
            return products.getContent().stream();
        });
    }

    private void configureCategoryGrid() {
        // Style the grid to match theme
        categoryGrid.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.1)")
                .set("background", "white")
                .set("--lumo-primary-text-color", "#0A9396");

        categoryGrid.addColumn(ProductCategoryDTO::id).setHeader("ID").setAutoWidth(true);
        categoryGrid.addColumn(ProductCategoryDTO::name).setHeader("Category Name").setAutoWidth(true);

        if (isAdmin) {
            categoryGrid.addComponentColumn(dto -> {
                HorizontalLayout actions = new HorizontalLayout();
                actions.setSpacing(true);

                Button editBtn = new Button("Edit");
                editBtn.getStyle().set("color", "#0A9396");
                editBtn.addClickListener(e -> openCategoryDialog(dto));

                Button deleteBtn = new Button("Delete");
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                deleteBtn.getStyle().set("color", "#FF7F11");
                deleteBtn.addClickListener(e -> {
                    categoryService.delete(dto.id());
                    refreshCategories();
                    updateAddProductButtonState();
                });

                actions.add(editBtn, deleteBtn);
                return actions;
            }).setHeader("Actions").setAutoWidth(true);
        } else {
            categoryGrid.addComponentColumn(dto -> {
                Button viewBtn = new Button("View Details");
                viewBtn.getStyle().set("color", "#0A9396");
                viewBtn.addClickListener(e -> openViewCategoryDialog(dto));
                return viewBtn;
            }).setHeader("Action").setAutoWidth(true);
        }

        // ✅ Lazy pagination
        categoryGrid.setItems(query -> {
            int page = query.getPage();
            Long companyId = securityService.getAuthenticatedUser()
                    .map(u -> u.getCompany().getId())
                    .orElseThrow();
            Page<ProductCategoryDTO> categories = categoryService.getCategoriesByCompany(companyId, page, PAGE_SIZE);
            return categories.getContent().stream();
        });
    }

    private void refreshProducts() {
        updateAddProductButtonState();
        productGrid.getDataProvider().refreshAll();
    }

    private void refreshCategories() {
        categoryGrid.getDataProvider().refreshAll();
    }

    private void openCategoryDialogOrign(ProductCategoryDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

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

        H4 dialogHeader = new H4(dto == null ? "Add Category" : "Edit Category");
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

        TextField nameField = new TextField("Category Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        if (dto != null) nameField.setValue(dto.name());

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Save", e -> {
            if (nameField.isEmpty()) {
                nameField.setInvalid(true);
                nameField.setErrorMessage("Category name is required");
                return;
            }
            try {
                ProductCategory category = dto == null ? new ProductCategory() : categoryService.findById(dto.id());
                category.setName(nameField.getValue().trim());
                Long companyId = securityService.getAuthenticatedUser().map(u -> u.getCompany().getId()).orElseThrow();
                category.setCompany(companyService.findById(companyId));
                categoryService.save(category);
                dialog.close();
                refreshCategories();
                updateAddProductButtonState();
                showSuccessNotification(dto == null ? "Category created" : "Category updated");
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancel = new Button("Cancel", e -> dialog.close());
        cancel.getStyle().set("color", "#FF7F11");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancel, save);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();

        dialogLayout.add(headerLayout, formLayout, buttonLayout);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }
    private void openCategoryDialog(ProductCategoryDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.getElement().getStyle().set("padding", "0"); // remove default Vaadin padding

        // === Outer container ===
        Div dialogContainer = new Div();
        dialogContainer.getStyle()
                .set("background", "white")
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 8px 24px rgba(0,0,0,0.2)")
                .set("border", "1px solid rgba(255,127,17,0.2)")
                .set("overflow", "hidden");

        // === Gradient header ===
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                .set("padding", "0.8rem 1.2rem")
                .set("color", "white");

        H4 dialogHeader = new H4(dto == null ? "Add Category" : "Edit Category");
        dialogHeader.getStyle()
                .set("margin", "0")
                .set("color", "white") // visible text
                .set("font-weight", "700")
                .set("font-size", "1.2rem");

        Button closeButton = new Button("×", e -> dialog.close());
        closeButton.getStyle()
                .set("color", "white")
                .set("font-size", "1.3rem")
                .set("background", "transparent")
                .set("border", "none")
                .set("cursor", "pointer");
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        headerLayout.add(dialogHeader, closeButton);

        // === Content area ===
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setMargin(false);
        dialogLayout.getStyle()
                .set("padding", "1.5rem")
                .set("background", "white");

        TextField nameField = new TextField("Category Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        if (dto != null) nameField.setValue(dto.name());

        FormLayout formLayout = new FormLayout(nameField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // === Buttons ===
        Button save = new Button("Save", e -> {
            if (nameField.isEmpty()) {
                nameField.setInvalid(true);
                nameField.setErrorMessage("Category name is required");
                return;
            }
            try {
                ProductCategory category = dto == null ? new ProductCategory() : categoryService.findById(dto.id());
                category.setName(nameField.getValue().trim());
                Long companyId = securityService.getAuthenticatedUser()
                        .map(u -> u.getCompany().getId()).orElseThrow();
                category.setCompany(companyService.findById(companyId));
                categoryService.save(category);
                dialog.close();
                refreshCategories();
                updateAddProductButtonState();
                showSuccessNotification(dto == null ? "Category created" : "Category updated");
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancel = new Button("Cancel", e -> dialog.close());
        cancel.getStyle()
                .set("color", "#FF7F11")
                .set("font-weight", "600");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancel, save);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);

        dialogLayout.add(formLayout, buttonLayout);

        // === Combine header + content ===
        dialogContainer.add(headerLayout, dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

    private void openProductDialogOriginal(ProductDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");

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

        H4 dialogHeader = new H4(dto == null ? "Add Product" : "Edit Product");
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

        TextField nameField = new TextField("Product Name");
        TextField unitField = new TextField("Unit of Measurement");

        Long companyId = securityService.getAuthenticatedUser().map(u -> u.getCompany().getId()).orElseThrow();
        List<ProductCategoryDTO> categories = categoryService.getCategoriesByCompany(companyId, 0, 100).getContent();

        ComboBox<ProductCategoryDTO> categoryBox = new ComboBox<>("Category");
        categoryBox.setItems(categories);
        categoryBox.setItemLabelGenerator(ProductCategoryDTO::name);
        categoryBox.setWidthFull();

        if (dto != null) {
            nameField.setValue(dto.name());
            unitField.setValue(dto.unitOfMeasurement());
            categories.stream()
                    .filter(c -> c.name().equals(dto.categoryName()))
                    .findFirst()
                    .ifPresent(categoryBox::setValue);
        }

        FormLayout formLayout = new FormLayout();
        formLayout.add(categoryBox, nameField, unitField);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(categoryBox, 2);

        Button save = new Button("Save", e -> {
            if (nameField.isEmpty() || unitField.isEmpty() || categoryBox.isEmpty()) {
                showErrorNotification("All fields are required");
                return;
            }
            try {
                Product product = dto == null ? new Product() : productService.findById(dto.id());
                product.setName(nameField.getValue().trim());
                product.setUnitOfMeasurement(unitField.getValue().trim());
                product.setProductCategory(categoryService.findById(categoryBox.getValue().id()));
                product.setCompany(companyService.findById(companyId));
                product.setActive(true);
                productService.save(product);
                dialog.close();
                refreshProducts();
                showSuccessNotification(dto == null ? "Product created" : "Product updated");
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancel = new Button("Cancel", e -> dialog.close());
        cancel.getStyle().set("color", "#FF7F11");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancel, save);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();

        dialogLayout.add(headerLayout, formLayout, buttonLayout);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }
    private void openProductDialog(ProductDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("520px");

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
        dialogLayout.setPadding(true);
        dialogLayout.setMargin(false);

        // === Header with background color (fixes invisible text issue) ===
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setPadding(true);
        headerLayout.getStyle()
                .set("background", "linear-gradient(135deg, #FF7F11, #0A9396)")
                .set("color", "white")
                .set("padding", "0.75rem 1rem")
                .set("align-items", "center");

        H4 dialogHeader = new H4(dto == null ? "Add Product" : "Edit Product");
        dialogHeader.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        Button closeButton = new Button("✕", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.getStyle()
                .set("color", "white")
                .set("font-size", "1.2em");

        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.add(dialogHeader, closeButton);

        // === Form Fields ===
        TextField nameField = new TextField("Product Name");
        TextField unitField = new TextField("Unit of Measurement");

        Long companyId = securityService.getAuthenticatedUser()
                .map(u -> u.getCompany().getId())
                .orElseThrow();

        List<ProductCategoryDTO> categories = categoryService
                .getCategoriesByCompany(companyId, 0, 100)
                .getContent();

        ComboBox<ProductCategoryDTO> categoryBox = new ComboBox<>("Category");
        categoryBox.setItems(categories);
        categoryBox.setItemLabelGenerator(ProductCategoryDTO::name);
        categoryBox.setWidthFull();

        if (dto != null) {
            nameField.setValue(dto.name());
            unitField.setValue(dto.unitOfMeasurement());
            categories.stream()
                    .filter(c -> c.name().equals(dto.categoryName()))
                    .findFirst()
                    .ifPresent(categoryBox::setValue);
        }

        FormLayout formLayout = new FormLayout();
        formLayout.add(categoryBox, nameField, unitField);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(categoryBox, 2);
        formLayout.getStyle().set("padding", "0.75rem 1rem");

        // === Buttons ===
        Button save = new Button("Save", e -> {
            if (nameField.isEmpty() || unitField.isEmpty() || categoryBox.isEmpty()) {
                showErrorNotification("All fields are required");
                return;
            }
            try {
                Product product = dto == null ? new Product() : productService.findById(dto.id());
                product.setName(nameField.getValue().trim());
                product.setUnitOfMeasurement(unitField.getValue().trim());
                product.setProductCategory(categoryService.findById(categoryBox.getValue().id()));
                product.setCompany(companyService.findById(companyId));
                product.setActive(true);
                productService.save(product);
                dialog.close();
                refreshProducts();
                showSuccessNotification(dto == null ? "Product created" : "Product updated");
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.getStyle()
                .set("background", "linear-gradient(135deg, #0A9396, #0DAAAD)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("border", "none");

        Button cancel = new Button("Cancel", e -> dialog.close());
        cancel.getStyle().set("color", "#FF7F11");

        HorizontalLayout buttonLayout = new HorizontalLayout(cancel, save);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.getStyle().set("padding", "0.5rem 1rem 1rem 1rem");

        // === Final Assembly ===
        dialogLayout.add(headerLayout, formLayout, buttonLayout);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

    private void updateAddProductButtonState() {
        if (!isAdmin || addProductButton == null) return;
        Long companyId = securityService.getAuthenticatedUser().map(u -> u.getCompany().getId()).orElseThrow();
        boolean hasCategories = !categoryService.getCategoriesByCompany(companyId, 0, 1).getContent().isEmpty();
        addProductButton.setEnabled(hasCategories);
        addProductButton.getElement().setProperty("title",
                hasCategories ? "" : "Add a category first before adding products");
    }

    private void showSuccessNotification(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void openViewProductDialog(ProductDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

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

        H4 dialogHeader = new H4("Product Details");
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

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        content.add(
                createDetailRow("Product Name", dto.name()),
                createDetailRow("Category", dto.categoryName()),
                createDetailRow("Unit", dto.unitOfMeasurement()),
                createDetailRow("Active", dto.active() ? "Yes" : "No")
        );

        dialogLayout.add(headerLayout, content);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

    private void openViewCategoryDialog(ProductCategoryDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

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

        H4 dialogHeader = new H4("Category Details");
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

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        content.add(
                createDetailRow("Category Name", dto.name()),
                createDetailRow("ID", String.valueOf(dto.id()))
        );

        dialogLayout.add(headerLayout, content);
        dialogContainer.add(dialogLayout);
        dialog.add(dialogContainer);
        dialog.open();
    }

    private HorizontalLayout createDetailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#0A9396");

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("color", "var(--lumo-body-text-color)");

        row.add(labelSpan, valueSpan);
        return row;
    }
}
/*
package com.benguides.frontend.views.inventory;

import com.benguides.dtos.ProductCategoryDTO;
import com.benguides.dtos.ProductDTO;
import com.benguides.models.Product;
import com.benguides.models.ProductCategory;
import com.benguides.security.SecurityService;
import com.benguides.services.CompanyService;
import com.benguides.services.ProductCategoryService;
import com.benguides.services.ProductService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@PageTitle("Inventory")
@Route(value = "inventory", layout = com.benguides.frontend.layout.MainLayout.class)
@RolesAllowed({
        "ROLE_COMPANY_ADMIN",
        "ROLE_BRANCH_MANAGER",
        "ROLE_SHIFT_SUPERVISOR",
        "ROLE_SHIFT_ATTENDANT"
})
@Component
@UIScope
@RequiredArgsConstructor
public class ProductCategoryAndProductView extends VerticalLayout {

    private final ProductService productService;
    private final ProductCategoryService categoryService;
    private final CompanyService companyService;
    private final SecurityService securityService;

    private final Grid<ProductDTO> productGrid = new Grid<>(ProductDTO.class, false);
    private final Grid<ProductCategoryDTO> categoryGrid = new Grid<>(ProductCategoryDTO.class, false);

    // Changed order: Categories first, then Products
    private final Tab categoriesTab = new Tab("Categories");
    private final Tab productsTab = new Tab("Products");
    private final Tabs tabs = new Tabs(categoriesTab, productsTab); // Categories on left, Products on right

    private final VerticalLayout contentLayout = new VerticalLayout();
    private final HorizontalLayout actionButtonsLayout = new HorizontalLayout();
    private final boolean isAdmin;
    private Button addProductButton;
    private Button addCategoryButton;

    private int productPage = 0;
    private int categoryPage = 0;
    private static final int PAGE_SIZE = 20;

    @Autowired
    public ProductCategoryAndProductView(ProductService productService,
                                         ProductCategoryService categoryService,
                                         CompanyService companyService,
                                         SecurityService securityService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.companyService = companyService;
        this.securityService = securityService;
        this.isAdmin = securityService.hasRole("ROLE_COMPANY_ADMIN");

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header section
        VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setPadding(false);
        headerLayout.setSpacing(false);
        headerLayout.setWidthFull();

        H2 header = new H2("Inventory Management");
        header.getStyle()
                .set("margin", "0")
                .set("padding-bottom", "1rem")
                .set("color", "var(--lumo-primary-text-color)");

        headerLayout.add(header);

        // Configure action buttons layout
        configureActionButtonsLayout();

        // Configure tabs with styling - Categories will be first (left), Products second (right)
        configureTabs();

        // Configure grids
        configureGrids();

        // Content layout styling
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);

        // Main layout organization
        add(headerLayout);
        if (isAdmin) {
            add(actionButtonsLayout);
        }
        add(tabs, contentLayout);

        // Apply overall styling
        getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-contrast-5pct)");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Start with Categories tab selected (leftmost tab)
        updateContent(categoriesTab);
        tabs.setSelectedTab(categoriesTab);
    }

    private void configureActionButtonsLayout() {
        actionButtonsLayout.setWidthFull();
        actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        actionButtonsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        actionButtonsLayout.setSpacing(true);
        actionButtonsLayout.getStyle()
                .set("margin-bottom", "1rem")
                .set("padding", "0");

        if (isAdmin) {
            addCategoryButton = new Button("Add Category", e -> openCategoryDialog(null));
            addCategoryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addCategoryButton.getStyle().set("margin-right", "auto");

            addProductButton = new Button("Add Product", e -> openProductDialog(null));
            addProductButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addProductButton.getStyle().set("margin-left", "0");

            actionButtonsLayout.add(addCategoryButton, addProductButton);
        }
    }

    private void configureTabs() {
        tabs.addSelectedChangeListener(event -> updateContent(event.getSelectedTab()));
        tabs.getStyle()
                .set("margin-bottom", "1rem")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        // Style the tabs individually
        tabs.setWidth("fit-content");
        tabs.getChildren().forEach(tab -> {
            tab.getElement().getStyle()
                    .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                    .set("font-weight", "500");
        });
    }

    private void updateContent(Tab selectedTab) {
        contentLayout.removeAll();
        if (selectedTab.equals(productsTab)) {
            refreshProducts();
            contentLayout.add(productGrid);
            // Show/hide appropriate buttons
            if (isAdmin) {
                addProductButton.setVisible(true);
                addCategoryButton.setVisible(false);
            }
        } else {
            // Categories tab is selected (this is now the default/first tab)
            refreshCategories();
            contentLayout.add(categoryGrid);
            // Show/hide appropriate buttons
            if (isAdmin) {
                addProductButton.setVisible(false);
                addCategoryButton.setVisible(true);
            }
        }
    }

    private void configureGrids() {
        configureProductGrid();
        configureCategoryGrid();
    }

    private void configureProductGrid() {
        // Style the grid
        productGrid.getStyle()
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        productGrid.addColumn(ProductDTO::id).setHeader("ID").setAutoWidth(true);
        productGrid.addColumn(ProductDTO::name).setHeader("Product Name").setAutoWidth(true);
        productGrid.addColumn(ProductDTO::categoryName).setHeader("Category").setAutoWidth(true);
        productGrid.addColumn(ProductDTO::unitOfMeasurement).setHeader("Unit").setAutoWidth(true);
        productGrid.addColumn(dto -> dto.active() ? "Yes" : "No").setHeader("Active").setAutoWidth(true);

        if (isAdmin) {
            productGrid.addComponentColumn(dto -> {
                HorizontalLayout actions = new HorizontalLayout();
                actions.setSpacing(true);

                Button editBtn = new Button("Edit", e -> openProductDialog(dto));
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editBtn.getStyle().set("font-size", "var(--lumo-font-size-s)");

                Button deleteBtn = new Button("Delete", e -> {
                    productService.delete(dto.id());
                    refreshProducts();
                });
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                deleteBtn.getStyle().set("font-size", "var(--lumo-font-size-s)");

                actions.add(editBtn, deleteBtn);
                return actions;
            }).setHeader("Actions").setAutoWidth(true);
        } else {
            productGrid.addComponentColumn(dto -> {
                Button viewBtn = new Button("View Details", e -> openViewProductDialog(dto));
                viewBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                viewBtn.getStyle().set("font-size", "var(--lumo-font-size-s)");
                return viewBtn;
            }).setHeader("Action").setAutoWidth(true);
        }

        // ✅ Lazy pagination
        productGrid.setItems(query -> {
            int page = query.getPage();
            Long companyId = securityService.getAuthenticatedUser()
                    .map(u -> u.getCompany().getId())
                    .orElseThrow();
            Page<ProductDTO> products = productService.getProductsByCompany(companyId, page, PAGE_SIZE);
            return products.getContent().stream();
        });
    }

    private void configureCategoryGrid() {
        // Style the grid
        categoryGrid.getStyle()
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        categoryGrid.addColumn(ProductCategoryDTO::id).setHeader("ID").setAutoWidth(true);
        categoryGrid.addColumn(ProductCategoryDTO::name).setHeader("Category Name").setAutoWidth(true);

        if (isAdmin) {
            categoryGrid.addComponentColumn(dto -> {
                HorizontalLayout actions = new HorizontalLayout();
                actions.setSpacing(true);

                Button editBtn = new Button("Edit", e -> openCategoryDialog(dto));
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editBtn.getStyle().set("font-size", "var(--lumo-font-size-s)");

                Button deleteBtn = new Button("Delete", e -> {
                    categoryService.delete(dto.id());
                    refreshCategories();
                    updateAddProductButtonState();
                });
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                deleteBtn.getStyle().set("font-size", "var(--lumo-font-size-s)");

                actions.add(editBtn, deleteBtn);
                return actions;
            }).setHeader("Actions").setAutoWidth(true);
        } else {
            categoryGrid.addComponentColumn(dto -> {
                Button viewBtn = new Button("View Details", e -> openViewCategoryDialog(dto));
                viewBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                viewBtn.getStyle().set("font-size", "var(--lumo-font-size-s)");
                return viewBtn;
            }).setHeader("Action").setAutoWidth(true);
        }

        // ✅ Lazy pagination
        categoryGrid.setItems(query -> {
            int page = query.getPage();
            Long companyId = securityService.getAuthenticatedUser()
                    .map(u -> u.getCompany().getId())
                    .orElseThrow();
            Page<ProductCategoryDTO> categories = categoryService.getCategoriesByCompany(companyId, page, PAGE_SIZE);
            return categories.getContent().stream();
        });
    }

    private void refreshProducts() {
        updateAddProductButtonState();
        productGrid.getDataProvider().refreshAll();
    }

    private void refreshCategories() {
        categoryGrid.getDataProvider().refreshAll();
    }

    private void openCategoryDialog(ProductCategoryDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(dto == null ? "Add Category" : "Edit Category");
        dialog.setWidth("400px");

        TextField nameField = new TextField("Category Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        if (dto != null) nameField.setValue(dto.name());

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Save", e -> {
            if (nameField.isEmpty()) {
                nameField.setInvalid(true);
                nameField.setErrorMessage("Category name is required");
                return;
            }
            try {
                ProductCategory category = dto == null ? new ProductCategory() : categoryService.findById(dto.id());
                category.setName(nameField.getValue().trim());
                Long companyId = securityService.getAuthenticatedUser().map(u -> u.getCompany().getId()).orElseThrow();
                category.setCompany(companyService.findById(companyId));
                categoryService.save(category);
                dialog.close();
                refreshCategories();
                updateAddProductButtonState();
                showSuccessNotification(dto == null ? "Category created" : "Category updated");
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(save, cancel);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void openProductDialog(ProductDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(dto == null ? "Add Product" : "Edit Product");
        dialog.setWidth("500px");

        TextField nameField = new TextField("Product Name");
        TextField unitField = new TextField("Unit of Measurement");

        Long companyId = securityService.getAuthenticatedUser().map(u -> u.getCompany().getId()).orElseThrow();
        List<ProductCategoryDTO> categories = categoryService.getCategoriesByCompany(companyId, 0, 100).getContent();

        ComboBox<ProductCategoryDTO> categoryBox = new ComboBox<>("Category");
        categoryBox.setItems(categories);
        categoryBox.setItemLabelGenerator(ProductCategoryDTO::name);
        categoryBox.setWidthFull();

        if (dto != null) {
            nameField.setValue(dto.name());
            unitField.setValue(dto.unitOfMeasurement());
            categories.stream()
                    .filter(c -> c.name().equals(dto.categoryName()))
                    .findFirst()
                    .ifPresent(categoryBox::setValue);
        }

        FormLayout formLayout = new FormLayout();
        formLayout.add(categoryBox, nameField, unitField);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.setColspan(categoryBox, 2);

        Button save = new Button("Save", e -> {
            if (nameField.isEmpty() || unitField.isEmpty() || categoryBox.isEmpty()) {
                showErrorNotification("All fields are required");
                return;
            }
            try {
                Product product = dto == null ? new Product() : productService.findById(dto.id());
                product.setName(nameField.getValue().trim());
                product.setUnitOfMeasurement(unitField.getValue().trim());
                product.setProductCategory(categoryService.findById(categoryBox.getValue().id()));
                product.setCompany(companyService.findById(companyId));
                product.setActive(true);
                productService.save(product);
                dialog.close();
                refreshProducts();
                showSuccessNotification(dto == null ? "Product created" : "Product updated");
            } catch (Exception ex) {
                showErrorNotification("Error: " + ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(save, cancel);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void updateAddProductButtonState() {
        if (!isAdmin || addProductButton == null) return;
        Long companyId = securityService.getAuthenticatedUser().map(u -> u.getCompany().getId()).orElseThrow();
        boolean hasCategories = !categoryService.getCategoriesByCompany(companyId, 0, 1).getContent().isEmpty();
        addProductButton.setEnabled(hasCategories);
        addProductButton.getElement().setProperty("title",
                hasCategories ? "" : "Add a category first before adding products");
    }

    private void showSuccessNotification(String msg) {
        Notification.show(msg, 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showErrorNotification(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void openViewProductDialog(ProductDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Product Details");
        dialog.setWidth("400px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        content.add(
                createDetailRow("Product Name", dto.name()),
                createDetailRow("Category", dto.categoryName()),
                createDetailRow("Unit", dto.unitOfMeasurement()),
                createDetailRow("Active", dto.active() ? "Yes" : "No")
        );

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.getStyle().set("margin-top", "var(--lumo-space-m)");

        VerticalLayout dialogLayout = new VerticalLayout(content, closeButton);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void openViewCategoryDialog(ProductCategoryDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Category Details");
        dialog.setWidth("400px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        content.add(
                createDetailRow("Category Name", dto.name()),
                createDetailRow("ID", String.valueOf(dto.id()))
        );

        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.getStyle().set("margin-top", "var(--lumo-space-m)");

        VerticalLayout dialogLayout = new VerticalLayout(content, closeButton);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private HorizontalLayout createDetailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("color", "var(--lumo-body-text-color)");

        row.add(labelSpan, valueSpan);
        return row;
    }
}
*/
