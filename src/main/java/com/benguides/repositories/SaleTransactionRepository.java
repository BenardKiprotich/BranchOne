package com.benguides.repositories;

import com.benguides.dtos.SaleTransactionDTO;
import com.benguides.models.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {

    @Query("SELECT new com.benguides.dtos.SaleTransactionDTO(" +
            "s.id, s.transactionDate, s.litresOpeningReading, s.litresClosingReading, " +
            "s.cashOpeningReading, s.cashClosingReading, s.quantity, s.totalAmount, " +
            "s.unitPrice, s.buyingPrice, s.costOfSales, s.shiftSession, " +
            "s.product.name, s.branch.name, s.branch.id, s.product.id, s.createdAt) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId")
    List<SaleTransactionDTO> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT new com.benguides.dtos.SaleTransactionDTO(" +
            "s.id, s.transactionDate, s.litresOpeningReading, s.litresClosingReading, " +
            "s.cashOpeningReading, s.cashClosingReading, s.quantity, s.totalAmount, " +
            "s.unitPrice, s.buyingPrice, s.costOfSales, s.shiftSession, " +
            "s.product.name, s.branch.name, s.branch.id, s.product.id, s.createdAt) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId " +
            "AND (:startDate IS NULL OR s.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR s.transactionDate <= :endDate)")
    Page<SaleTransactionDTO> findByCompanyIdAndDateRangePaged(@Param("companyId") Long companyId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate,
                                                              Pageable pageable);

    @Query("SELECT new com.benguides.dtos.SaleTransactionDTO(" +
            "s.id, s.transactionDate, s.litresOpeningReading, s.litresClosingReading, " +
            "s.cashOpeningReading, s.cashClosingReading, s.quantity, s.totalAmount, " +
            "s.unitPrice, s.buyingPrice, s.costOfSales, s.shiftSession, " +
            "s.product.name, s.branch.name, s.branch.id, s.product.id, s.createdAt) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId AND s.transactionDate = :date")
    List<SaleTransactionDTO> findByCompanyAndDate(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT new com.benguides.dtos.SaleTransactionDTO(" +
            "s.id, s.transactionDate, s.litresOpeningReading, s.litresClosingReading, " +
            "s.cashOpeningReading, s.cashClosingReading, s.quantity, s.totalAmount, " +
            "s.unitPrice, s.buyingPrice, s.costOfSales, s.shiftSession, " +
            "s.product.name, s.branch.name, s.branch.id, s.product.id, s.createdAt) " +
            "FROM SaleTransaction s WHERE s.branch.id = :branchId AND s.transactionDate = :date")
    List<SaleTransactionDTO> findByBranchAndDate(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    // For sales per branch (all time)
    @Query("SELECT s.branch.name, SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId GROUP BY s.branch.name")
    List<Object[]> getSalesPerBranch(@Param("companyId") Long companyId);

    // For sales per branch between dates
    @Query("SELECT s.branch.name, SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId AND s.transactionDate BETWEEN :start AND :end GROUP BY s.branch.name")
    List<Object[]> getSalesPerBranchBetween(@Param("companyId") Long companyId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // For daily sales per product
    @Query("SELECT s.product.name, SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId AND s.transactionDate = :date GROUP BY s.product.name")
    List<Object[]> getDailySalesPerProduct(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    // For daily sales per branch
    @Query("SELECT s.branch.name, SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId AND s.transactionDate = :date GROUP BY s.branch.name")
    List<Object[]> getDailySalesPerBranch(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    // For sales per product (all time)
    @Query("SELECT s.product.name, SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId GROUP BY s.product.name")
    List<Object[]> getSalesPerProduct(@Param("companyId") Long companyId);

    // For sales per product between dates
    @Query("SELECT s.product.name, SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId AND s.transactionDate BETWEEN :start AND :end GROUP BY s.product.name")
    List<Object[]> getSalesPerProductBetween(@Param("companyId") Long companyId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // For company daily sales summary
    @Query("SELECT SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId AND s.transactionDate = :date")
    Object[] getDailyCompanySalesSummary(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    // For company total sales summary (all time)
    @Query("SELECT SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId")
    Object[] getCompanyTotalSalesSummary(@Param("companyId") Long companyId);

    // For sales summary between dates
    @Query("SELECT SUM(s.totalAmount), SUM(s.costOfSales), SUM(s.quantity) " +
            "FROM SaleTransaction s WHERE s.company.id = :companyId AND s.transactionDate BETWEEN :start AND :end")
    Object[] getSalesSummaryBetween(@Param("companyId") Long companyId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}