package com.benguides.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CompanyCreateRequest {
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Company address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "Company phone is required")
    //@Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Phone number must be 10-13 digits and may start with +")
    @Pattern(regexp = "^(\\+?\\d{10,13})$", message = "Phone number must be 10–13 digits, may include country code")
    private String companyPhone;

    @NotBlank(message = "Company email is required")
    @Email(message = "Please provide a valid company email address")
    private String email;

    @NotBlank(message = "Admin first name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name can only contain letters and spaces")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name can only contain letters and spaces")
    private String adminLastName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String adminUsername;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Please provide a valid admin email address")
    private String adminEmail;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?\\d{10,13})$", message = "Phone number must be 10–13 digits, may include country code")
    private String adminPhone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must contain at least one digit, one lowercase, one uppercase, one special character and no spaces")
    private String adminPassword;
}
