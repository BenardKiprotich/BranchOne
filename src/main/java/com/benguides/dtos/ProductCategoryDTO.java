package com.benguides.dtos;

public record ProductCategoryDTO(
        Long id,
        String name,
        String companyName,
        Long companyId
) {}
