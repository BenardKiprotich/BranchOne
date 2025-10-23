package com.benguides.services;

import com.benguides.dtos.ExpenseTransactionDTO;
import com.benguides.models.Branch;
import com.benguides.models.Company;
import com.benguides.models.ExpenseTransaction;
import com.benguides.models.ExpenseType;
import com.benguides.repositories.ExpenseTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseTransactionService {

    private final ExpenseTransactionRepository expenseTransactionRepository;

    public ExpenseTransaction createNew(Company company, Branch branch, ExpenseType expenseType, LocalDate date) {
        ExpenseTransaction transaction = new ExpenseTransaction();
        transaction.setCompany(company);
        transaction.setBranch(branch);
        transaction.setExpenseType(expenseType);
        transaction.setTransactionDate(date);
        return transaction;
    }

    public ExpenseTransaction save(ExpenseTransaction transaction) {
        return expenseTransactionRepository.save(transaction);
    }

    public void delete(Long id) {
        expenseTransactionRepository.deleteById(id);
    }

    public Page<ExpenseTransactionDTO> getAllByCompanyPaged(Long companyId, LocalDate startDate, LocalDate endDate, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("transactionDate").descending());
        return expenseTransactionRepository.findByCompanyIdAndDateRangePaged(companyId, startDate, endDate, pageable);
    }

    // Helper to convert List<Object[]> to Map<String, BigDecimal[]>
    private Map<String, BigDecimal[]> convertToMap(List<Object[]> data) {
        Map<String, BigDecimal[]> result = new HashMap<>();
        for (Object[] row : data) {
            String key = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            result.put(key, new BigDecimal[]{amount});
        }
        return result;
    }

    public Map<String, BigDecimal[]> getExpensesPerType(Long companyId) {
        return convertToMap(expenseTransactionRepository.getExpensesPerType(companyId));
    }

    public Map<String, BigDecimal[]> getExpensesPerTypeBetween(Long companyId, LocalDate start, LocalDate end) {
        return convertToMap(expenseTransactionRepository.getExpensesPerTypeBetween(companyId, start, end));
    }

    public Map<String, BigDecimal[]> getExpensesPerBranch(Long companyId) {
        return convertToMap(expenseTransactionRepository.getExpensesPerBranch(companyId));
    }

    public Map<String, BigDecimal[]> getExpensesPerBranchBetween(Long companyId, LocalDate start, LocalDate end) {
        return convertToMap(expenseTransactionRepository.getExpensesPerBranchBetween(companyId, start, end));
    }

    public BigDecimal[] getCompanyTotalExpensesSummary(Long companyId) {
        Object[] result = expenseTransactionRepository.getCompanyTotalExpensesSummary(companyId);
        return new BigDecimal[]{(BigDecimal) (result != null && result.length > 0 ? result[0] : BigDecimal.ZERO)};
    }

    public BigDecimal[] getExpensesSummaryBetween(Long companyId, LocalDate start, LocalDate end) {
        Object[] result = expenseTransactionRepository.getExpensesSummaryBetween(companyId, start, end);
        return new BigDecimal[]{(BigDecimal) (result != null && result.length > 0 ? result[0] : BigDecimal.ZERO)};
    }
}