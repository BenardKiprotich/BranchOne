package com.benguides.dtos;

import java.time.LocalDateTime;

public record ExpenseTypeDTO(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}