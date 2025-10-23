package com.benguides.config;

import com.benguides.models.User;
import com.benguides.repositories.UserRepository;
import com.benguides.services.RoleService;
import com.benguides.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@Transactional
public class DataLoader {

    @Bean
    CommandLineRunner loadData(RoleService roleService, UserRepository userRepo,
                               PasswordEncoder encoder, UserService userService) {
        return args -> {
            // Initialize roles and permissions
            roleService.initializeRoles();

            // Default super admin user
            if (userRepo.findByUsername("superadmin").isEmpty()) {
                User admin = new User();
                admin.setUsername("superadmin");
                admin.setEmail("superadmin@branchone.com");
                admin.setFirstName("System");
                admin.setLastName("Administrator");
                admin.setPassword("sadmin@123");
                admin.setMustChangePassword(false);

                var superAdminRole = roleService.findByName("ROLE_SUPER_ADMIN").get();
                admin.setRoles(Set.of(superAdminRole));
                userService.createUser(admin, Set.of(superAdminRole), null, null);
            }

            // Default company admin
            if (userRepo.findByUsername("companyadmin").isEmpty()) {
                User cadmin = new User();
                cadmin.setUsername("companyadmin");
                cadmin.setEmail("companyadmin@branchone.com");
                cadmin.setFirstName("Company");
                cadmin.setLastName("Admin");
                cadmin.setPassword("cadmin@123");
                cadmin.setMustChangePassword(false);

                var companyAdminRole = roleService.findByName("ROLE_COMPANY_ADMIN").get();
                cadmin.setRoles(Set.of(companyAdminRole));
                userService.createUser(cadmin, Set.of(companyAdminRole), null, null);
            }
        };
    }
}
