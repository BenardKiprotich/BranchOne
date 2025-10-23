package com.benguides.security;

import com.benguides.dtos.UserWithCompanyDTO;
import com.benguides.models.User;
import com.benguides.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityService {
    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Transactional(readOnly = true)
    public Optional<User> getAuthenticatedUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                return userRepository.findByUsernameWithCompanyAndBranch(username);
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUserOrThrow() {
        return getAuthenticatedUser()
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public Optional<UserWithCompanyDTO> getAuthenticatedUserWithCompany() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                return userRepository.findByUsernameWithCompanyAndBranch(username)
                        .map(this::convertToUserWithCompanyDTO);
            }
        }
        return Optional.empty();
    }

    private UserWithCompanyDTO convertToUserWithCompanyDTO(User user) {
        return new UserWithCompanyDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.getCompany() != null ? user.getCompany().getName() : null,
                user.getBranch() != null ? user.getBranch().getId() : null,
                user.getBranch() != null ? user.getBranch().getName() : null,
                user.getRoles().stream()
                        .map(role -> role.getName())
                        .sorted()
                        .toList(),
                user.getStatus()
        );
    }

    public void logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        SecurityContextHolder.clearContext();
    }

    public boolean hasPermission(String permission) {
        return getAuthenticatedUser()
                .map(user -> user.hasPermission(permission))
                .orElse(false);
    }

    public boolean hasRole(String role) {
        return getAuthenticatedUser()
                .map(user -> user.hasRole(role))
                .orElse(false);
    }
    public boolean hasAnyRole(String... roles) {
        return getAuthenticatedUser()
                .map(user -> {
                    for (String role : roles) {
                        if (user.hasRole(role)) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }
}