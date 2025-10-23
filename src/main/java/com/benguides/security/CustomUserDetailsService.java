package com.benguides.security;

import com.benguides.models.User;
import com.benguides.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.isDeleted()) {
            throw new UsernameNotFoundException("User account is deleted: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == User.Status.ACTIVE,
                true, // account non-expired
                true, // credentials non-expired
                user.getStatus() != User.Status.LOCKED, // account non-locked
                getAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        // Include both roles and their associated permissions as authorities
        return user.getRoles().stream()
                .flatMap(role -> {
                    // Stream roles and their permissions
                    Collection<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    // Add the role itself
                    authorities.add(new SimpleGrantedAuthority(role.getName()));
                    // Add all permissions associated with the role
                    role.getPermissions().forEach(permission ->
                            authorities.add(new SimpleGrantedAuthority(permission.getName())));
                    return authorities.stream();
                })
                .collect(Collectors.toList());
    }
}
