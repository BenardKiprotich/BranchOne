package com.benguides.dtos;

public record ProductDTO(
        Long id,
        String name,
        String categoryName,
        String unitOfMeasurement,
        boolean active,
        String companyName,
        Long companyId
) {}
