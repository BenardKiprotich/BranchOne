package com.benguides.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sale_transactions")
@EqualsAndHashCode(of = "id")
public class SaleTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Litres readings (formerly pump readings)
    @Column(precision = 12, scale = 3)
    private BigDecimal litresOpeningReading = BigDecimal.ZERO;

    @Column(precision = 12, scale = 3)
    private BigDecimal litresClosingReading = BigDecimal.ZERO;

    // Cash readings (new)
    @Column(precision = 14, scale = 2)
    private BigDecimal cashOpeningReading = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal cashClosingReading = BigDecimal.ZERO;

    // Quantity in litres (persisted for reporting)
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    // Unit selling price (calculated: totalAmount / quantity)
    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    // Total sales amount (cashClosing - cashOpening)
    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Buying price per unit (new)
    @Column(precision = 10, scale = 2)
    private BigDecimal buyingPrice = BigDecimal.ZERO;

    // Cost of sales (quantity * buyingPrice) (new)
    @Column(precision = 14, scale = 2)
    private BigDecimal costOfSales = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ShiftSession shiftSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    public enum ShiftSession {
        DAY,
        NIGHT
    }

    // Transient getters for calculated values (if needed in code)
    @Transient
    public BigDecimal getCalculatedQuantity() {
        return litresClosingReading.subtract(litresOpeningReading).max(BigDecimal.ZERO);
    }

    @Transient
    public BigDecimal getCalculatedTotalAmount() {
        return cashClosingReading.subtract(cashOpeningReading).max(BigDecimal.ZERO);
    }

    @PrePersist
    @PreUpdate
    private void onSave() {
        this.quantity = getCalculatedQuantity();
        this.totalAmount = getCalculatedTotalAmount();
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            this.unitPrice = totalAmount.divide(quantity, 2, RoundingMode.HALF_UP);
        } else {
            this.unitPrice = BigDecimal.ZERO;
        }
        this.costOfSales = quantity.multiply(buyingPrice);
    }
}