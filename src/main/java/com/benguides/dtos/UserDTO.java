package com.benguides.dtos;

import com.benguides.models.User;

import java.util.List;

public record UserDTO(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String companyName,
        String branchName,
        List<String> roles,
        User.Status status
) {}

