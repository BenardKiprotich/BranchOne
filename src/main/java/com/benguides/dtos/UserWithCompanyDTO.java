package com.benguides.dtos;

import com.benguides.models.User;

import java.util.List;

public record UserWithCompanyDTO(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Long companyId,
        String companyName,
        Long branchId,
        String branchName,
        List<String> roles,
        User.Status status
) {}
