package com.benguides.repositories;

import com.benguides.dtos.ExpenseTransactionDTO;
import com.benguides.models.ExpenseTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransaction, Long> {

    @Query("SELECT new com.benguides.dtos.ExpenseTransactionDTO(" +
            "e.id, e.transactionDate, e.comment, e.amount, " +
            "e.expenseType.name, e.expenseType.id, " +
            "e.branch.name, e.branch.id, e.createdAt) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId")
    List<ExpenseTransactionDTO> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT new com.benguides.dtos.ExpenseTransactionDTO(" +
            "e.id, e.transactionDate, e.comment, e.amount, " +
            "e.expenseType.name, e.expenseType.id, " +
            "e.branch.name, e.branch.id, e.createdAt) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId " +
            "AND (:startDate IS NULL OR e.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR e.transactionDate <= :endDate)")
    Page<ExpenseTransactionDTO> findByCompanyIdAndDateRangePaged(@Param("companyId") Long companyId,
                                                                 @Param("startDate") LocalDate startDate,
                                                                 @Param("endDate") LocalDate endDate,
                                                                 Pageable pageable);

    // For expenses per type (all time)
    @Query("SELECT e.expenseType.name, SUM(e.amount) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId GROUP BY e.expenseType.name")
    List<Object[]> getExpensesPerType(@Param("companyId") Long companyId);

    // For expenses per type between dates
    @Query("SELECT e.expenseType.name, SUM(e.amount) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId AND e.transactionDate BETWEEN :start AND :end GROUP BY e.expenseType.name")
    List<Object[]> getExpensesPerTypeBetween(@Param("companyId") Long companyId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // For expenses per branch (all time)
    @Query("SELECT e.branch.name, SUM(e.amount) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId GROUP BY e.branch.name")
    List<Object[]> getExpensesPerBranch(@Param("companyId") Long companyId);

    // For expenses per branch between dates
    @Query("SELECT e.branch.name, SUM(e.amount) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId AND e.transactionDate BETWEEN :start AND :end GROUP BY e.branch.name")
    List<Object[]> getExpensesPerBranchBetween(@Param("companyId") Long companyId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // For company total expenses summary (all time)
    @Query("SELECT SUM(e.amount) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId")
    Object[] getCompanyTotalExpensesSummary(@Param("companyId") Long companyId);

    // For expenses summary between dates
    @Query("SELECT SUM(e.amount) " +
            "FROM ExpenseTransaction e WHERE e.company.id = :companyId AND e.transactionDate BETWEEN :start AND :end")
    Object[] getExpensesSummaryBetween(@Param("companyId") Long companyId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}