package com.benguides.services;

import com.benguides.models.Permission;
import com.benguides.models.Role;
import com.benguides.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    public void initializeRoles() {
        permissionService.initializePermissions();

        // Super Admin Role
        Role superAdmin = roleRepository.findByName("ROLE_SUPER_ADMIN")
                .orElse(new Role(null, "ROLE_SUPER_ADMIN", "System Super Administrator", false, false, new HashSet<>()));
        superAdmin.setPermissions(new HashSet<>(permissionService.getAllPermissions()));
        roleRepository.save(superAdmin);

        // Company Admin Role
        Role companyAdmin = roleRepository.findByName("ROLE_COMPANY_ADMIN")
                .orElse(new Role(null, "ROLE_COMPANY_ADMIN", "Company Administrator", true, true, new HashSet<>()));
        Set<Permission> companyAdminPermissions = getCompanyAdminPermissions();
        companyAdmin.setPermissions(companyAdminPermissions);
        roleRepository.save(companyAdmin);

        // Branch Manager Role
        Role branchManager = roleRepository.findByName("ROLE_BRANCH_MANAGER")
                .orElse(new Role(null, "ROLE_BRANCH_MANAGER", "Branch Manager", true, true, new HashSet<>()));
        Set<Permission> branchManagerPermissions = getBranchManagerPermissions();
        branchManager.setPermissions(branchManagerPermissions);
        roleRepository.save(branchManager);

        // Shift Supervisor Role
        Role shiftSupervisor = roleRepository.findByName("ROLE_SHIFT_SUPERVISOR")
                .orElse(new Role(null, "ROLE_SHIFT_SUPERVISOR", "Shift Supervisor", true, true, new HashSet<>()));
        Set<Permission> shiftSupervisorPermissions = getShiftSupervisorPermissions();
        shiftSupervisor.setPermissions(shiftSupervisorPermissions);
        roleRepository.save(shiftSupervisor);

        // Shift Attendant Role
        Role shiftAttendant = roleRepository.findByName("ROLE_SHIFT_ATTENDANT")
                .orElse(new Role(null, "ROLE_SHIFT_ATTENDANT", "Shift Attendant", true, true, new HashSet<>()));
        Set<Permission> shiftAttendantPermissions = getShiftAttendantPermissions();
        shiftAttendant.setPermissions(shiftAttendantPermissions);
        roleRepository.save(shiftAttendant);
    }

    private Set<Permission> getCompanyAdminPermissions() {
        return new HashSet<>(Arrays.asList(
                getPermission("CREATE_USER"),
                getPermission("VIEW_USERS"),
                getPermission("EDIT_USER"),
                getPermission("DELETE_USER"),
                getPermission("ASSIGN_ROLES"),
                getPermission("VIEW_COMPANIES"),
                getPermission("EDIT_COMPANY"),
                getPermission("CREATE_BRANCH"),
                getPermission("VIEW_BRANCHES"),
                getPermission("EDIT_BRANCH"),
                getPermission("DELETE_BRANCH"),
                getPermission("APPROVE_BRANCH_ENTRY"),
                getPermission("CREATE_PRODUCT"),
                getPermission("DELETE_PRODUCT"),
                getPermission("EDIT_PRODUCT"),
                getPermission("VIEW_PRODUCTS"),
                getPermission("APPROVE_PRODUCT_CREATION"),
                getPermission("CREATE_STOCK"),
                getPermission("ADJUST_STOCK"),
                getPermission("VIEW_STOCK"),
                getPermission("APPROVE_STOCK_ENTRY"),
                getPermission("APPROVE_STOCK_ADJUSTMENT")
        ));
    }

    private Set<Permission> getBranchManagerPermissions() {
        return new HashSet<>(Arrays.asList(
                getPermission("VIEW_USERS"),
                getPermission("CREATE_USER"),
                getPermission("VIEW_BRANCHES"),
                getPermission("CREATE_PRODUCT"),
                getPermission("VIEW_PRODUCTS"),
                getPermission("CREATE_STOCK"),
                getPermission("ADJUST_STOCK"),
                getPermission("VIEW_STOCK"),
                getPermission("APPROVE_STOCK_ENTRY"),
                getPermission("APPROVE_STOCK_ADJUSTMENT")
        ));
    }

    private Set<Permission> getShiftSupervisorPermissions() {
        return new HashSet<>(Arrays.asList(
                getPermission("VIEW_PRODUCTS"),
                getPermission("CREATE_STOCK"),
                getPermission("VIEW_STOCK"),
                getPermission("ADJUST_STOCK")
        ));
    }

    private Set<Permission> getShiftAttendantPermissions() {
        return new HashSet<>(Arrays.asList(
                getPermission("VIEW_PRODUCTS"),
                getPermission("VIEW_STOCK")
        ));
    }

    private Permission getPermission(String name) {
        return permissionService.getAllPermissions().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Permission not found: " + name));
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    public Role save(Role role) {
        return roleRepository.save(role);
    }
}
