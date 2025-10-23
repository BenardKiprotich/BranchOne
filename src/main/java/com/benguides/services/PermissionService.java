package com.benguides.services;

import com.benguides.models.Permission;
import com.benguides.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;

    public void initializePermissions() {
        List<Permission> defaultPermissions = Arrays.asList(
                // User Management
                new Permission("CREATE_USER", "Create new users", "USER_MANAGEMENT"),
                new Permission("VIEW_USERS", "View users list", "USER_MANAGEMENT"),
                new Permission("EDIT_USER", "Edit user information", "USER_MANAGEMENT"),
                new Permission("DELETE_USER", "Delete users", "USER_MANAGEMENT"),
                new Permission("ASSIGN_ROLES", "Assign roles to users", "USER_MANAGEMENT"),

                // Company Management
                new Permission("CREATE_COMPANY", "Create new company", "COMPANY_MANAGEMENT"),
                new Permission("VIEW_COMPANIES", "View companies list", "COMPANY_MANAGEMENT"),
                new Permission("EDIT_COMPANY", "Edit company information", "COMPANY_MANAGEMENT"),
                new Permission("DELETE_COMPANY", "Delete company", "COMPANY_MANAGEMENT"),

                // Branch Management
                new Permission("CREATE_BRANCH", "Create new branch", "BRANCH_MANAGEMENT"),
                new Permission("VIEW_BRANCHES", "View branches list", "BRANCH_MANAGEMENT"),
                new Permission("EDIT_BRANCH", "Edit branch information", "BRANCH_MANAGEMENT"),
                new Permission("DELETE_BRANCH", "Delete branch", "BRANCH_MANAGEMENT"),
                new Permission("APPROVE_BRANCH_ENTRY", "Approve branch creation", "BRANCH_MANAGEMENT"),

                // Product Management
                new Permission("CREATE_PRODUCT", "Create new product", "PRODUCT_MANAGEMENT"),
                new Permission("DELETE_PRODUCT", "Delete product", "PRODUCT_MANAGEMENT"),
                new Permission("EDIT_PRODUCT", "Edit product information", "PRODUCT_MANAGEMENT"),
                new Permission("VIEW_PRODUCTS", "View products list", "PRODUCT_MANAGEMENT"),
                new Permission("APPROVE_PRODUCT_CREATION", "Approve product creation", "PRODUCT_MANAGEMENT"),

                // Stock Management
                new Permission("CREATE_STOCK", "Create stock entry", "STOCK_MANAGEMENT"),
                new Permission("ADJUST_STOCK", "Adjust stock levels", "STOCK_MANAGEMENT"),
                new Permission("VIEW_STOCK", "View stock information", "STOCK_MANAGEMENT"),
                new Permission("APPROVE_STOCK_ENTRY", "Approve stock entries", "STOCK_MANAGEMENT"),
                new Permission("APPROVE_STOCK_ADJUSTMENT", "Approve stock adjustments", "STOCK_MANAGEMENT"),

                // System Administration
                new Permission("MANAGE_ROLES", "Manage roles and permissions", "SYSTEM_ADMIN"),
                new Permission("VIEW_SYSTEM_LOGS", "View system logs", "SYSTEM_ADMIN"),
                new Permission("MANAGE_SYSTEM_SETTINGS", "Manage system settings", "SYSTEM_ADMIN")
        );

        for (Permission permission : defaultPermissions) {
            if (!permissionRepository.existsByName(permission.getName())) {
                permissionRepository.save(permission);
            }
        }
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public List<Permission> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategory(category);
    }
}
