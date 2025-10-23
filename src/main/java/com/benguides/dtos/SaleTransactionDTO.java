package com.benguides.dtos;

import com.benguides.models.SaleTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SaleTransactionDTO(
        Long id,
        LocalDate transactionDate,
        BigDecimal litresOpeningReading,
        BigDecimal litresClosingReading,
        BigDecimal cashOpeningReading,
        BigDecimal cashClosingReading,
        BigDecimal quantity,
        BigDecimal totalAmount,
        BigDecimal unitPrice,
        BigDecimal buyingPrice,
        BigDecimal costOfSales,
        //String shiftSession,
        SaleTransaction.ShiftSession shiftSession,
        String productName,
        String branchName,
        Long branchId,
        Long productId,
        LocalDateTime createdAt
) {}
