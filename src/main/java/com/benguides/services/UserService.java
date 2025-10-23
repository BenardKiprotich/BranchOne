package com.benguides.services;

import com.benguides.dtos.UserDTO;
import com.benguides.models.*;
import com.benguides.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public User createUser(@Valid User user, Set<Role> roles, Company company, Branch branch) {
        validateUserUniqueness(user.getUsername(), user.getEmail());

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(roles);
        user.setCompany(company);
        user.setBranch(branch);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User createCompanyAdmin(@Valid User user, Company company) {
        validateUserUniqueness(user.getUsername(), user.getEmail());

        Role companyAdminRole = roleService.findByName("ROLE_COMPANY_ADMIN")
                .orElseThrow(() -> new RuntimeException("Company Admin role not found"));

        user.setRoles(Set.of(companyAdminRole));
        user.setCompany(company);
        user.setBranch(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setMustChangePassword(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private void validateUserUniqueness(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email '" + email + "' is already registered");
        }
    }

    public List<UserDTO> getAllUsersAsDTOs() {
        return userRepository.findAllWithCompanyBranchAndRoles().stream()
                .map(u -> new UserDTO(
                        u.getId(),
                        u.getUsername(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail(),
                        u.getPhoneNumber(),
                        u.getCompany() != null ? u.getCompany().getName() : null,
                        u.getBranch() != null ? u.getBranch().getName() : null,
                        u.getRoles().stream()
                                .map(Role::getName)
                                .sorted()
                                .toList(),
                        u.getStatus()
                ))
                .toList();
    }

    public List<User> getUsersByCompany(Long companyId) {
        return userRepository.findByCompanyIdWithBranchAndRoles(companyId);
    }

    public List<User> getUsersByBranch(Long branchId) {
        return userRepository.findByBranchId(branchId);
    }

    public List<User> getCompanyLevelUsers(Long companyId) {
        return userRepository.findCompanyLevelUsers(companyId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public boolean userExists(String username, String email) {
        return userRepository.existsByUsername(username) || userRepository.existsByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void changePassword(Long userId, String newPassword) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setMustChangePassword(false);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found with ID: " + userId);
        }
    }

    public Optional<User> findByUsernameWithDetails(String username) {
        return userRepository.findByUsernameWithDetails(username);
    }
}