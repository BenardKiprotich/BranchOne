package com.benguides.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseTransactionDTO(
        Long id,
        LocalDate transactionDate,
        String comment,
        BigDecimal amount,
        String expenseTypeName,
        Long expenseTypeId,
        String branchName,
        Long branchId,
        LocalDateTime createdAt
) {}