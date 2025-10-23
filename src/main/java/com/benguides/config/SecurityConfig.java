package com.benguides.config;

import com.benguides.frontend.views.LoginView;
import com.benguides.repositories.UserRepository;
import com.benguides.security.CustomUserDetailsService;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.GrantedAuthority;

@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Let Vaadin configure its internals
        super.configure(http);

        // Disable CSRF for H2 console
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));

        // Allow H2 console in frames
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        // Configure the Vaadin login view
        setLoginView(http, LoginView.class);

        // Custom login success handler
        http.formLogin(form -> form
                .loginPage("/login")
                .successHandler(roleBasedSuccessHandler())
                .permitAll()
        );

        // Logout configuration
        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
        );
    }

    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            if (response.isCommitted()) return;

            String username = authentication.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                try {
                    if (user.isMustChangePassword()) {
                        response.sendRedirect(request.getContextPath() + "/change-password");
                        return;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error during authentication success handling", e);
                }
            });

            String redirectUrl = "/shift-attendant/dashboard";
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                switch (auth.getAuthority()) {
                    case "ROLE_SUPER_ADMIN" -> redirectUrl = "/super-admin/dashboard";
                    case "ROLE_COMPANY_ADMIN" -> redirectUrl = "/company-admin/dashboard";
                    case "ROLE_BRANCH_MANAGER" -> redirectUrl = "/branch-manager/dashboard";
                    case "ROLE_SHIFT_SUPERVISOR" -> redirectUrl = "/shift-supervisor/dashboard";
                }
            }

            if (!response.isCommitted()) {
                response.sendRedirect(request.getContextPath() + redirectUrl);
            }
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
