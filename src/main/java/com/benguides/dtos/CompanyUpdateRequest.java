package com.benguides.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CompanyUpdateRequest {
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Company address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "Company phone is required")
    //@Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Phone number must be 10-13 digits and may start with +")
    @Pattern(regexp = "^(\\+?\\d{10,13})$", message = "Phone number must be 10–13 digits, may include country code")
    private String phone;

    @NotBlank(message = "Company email is required")
    @Email(message = "Please provide a valid company email address")
    private String email;
}
