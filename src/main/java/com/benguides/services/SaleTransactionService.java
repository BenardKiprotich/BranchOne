package com.benguides.services;

import com.benguides.dtos.SaleTransactionDTO;
import com.benguides.models.Branch;
import com.benguides.models.Company;
import com.benguides.models.Product;
import com.benguides.models.SaleTransaction;
import com.benguides.repositories.BranchRepository;
import com.benguides.repositories.ProductRepository;
import com.benguides.repositories.SaleTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleTransactionService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public SaleTransaction save(SaleTransaction saleTransaction) {
        return saleTransactionRepository.save(saleTransaction);
    }

    public List<SaleTransactionDTO> getAllByCompany(Long companyId) {
        return saleTransactionRepository.findByCompanyId(companyId);
    }

    public Page<SaleTransactionDTO> getAllByCompanyPaged(Long companyId, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return saleTransactionRepository.findByCompanyIdAndDateRangePaged(companyId, startDate, endDate, pageable);
    }

    public List<SaleTransactionDTO> getDailySales(Long companyId, LocalDate date) {
        return saleTransactionRepository.findByCompanyAndDate(companyId, date);
    }

    public List<SaleTransactionDTO> getDailySalesByBranch(Long branchId, LocalDate date) {
        return saleTransactionRepository.findByBranchAndDate(branchId, date);
    }

    public Optional<SaleTransaction> findById(Long id) {
        return saleTransactionRepository.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        saleTransactionRepository.deleteById(id);
    }

    // Analytics methods
    public Map<String, BigDecimal[]> getSalesPerBranch(Long companyId) {
        return toMap(saleTransactionRepository.getSalesPerBranch(companyId));
    }

    public Map<String, BigDecimal[]> getSalesPerBranchBetween(Long companyId, LocalDate start, LocalDate end) {
        return toMap(saleTransactionRepository.getSalesPerBranchBetween(companyId, start, end));
    }

    public Map<String, BigDecimal[]> getSalesPerProductBetween(Long companyId, LocalDate start, LocalDate end) {
        return toMap(saleTransactionRepository.getSalesPerProductBetween(companyId, start, end));
    }

    public Map<String, BigDecimal[]> getDailySalesPerProduct(Long companyId, LocalDate date) {
        return toMap(saleTransactionRepository.getDailySalesPerProduct(companyId, date));
    }

    public Map<String, BigDecimal[]> getDailySalesPerBranch(Long companyId, LocalDate date) {
        return toMap(saleTransactionRepository.getDailySalesPerBranch(companyId, date));
    }

    public Map<String, BigDecimal[]> getSalesPerProduct(Long companyId) {
        return toMap(saleTransactionRepository.getSalesPerProduct(companyId));
    }

    public BigDecimal[] getDailyCompanySalesSummary(Long companyId, LocalDate date) {
        return toBigDecimalArray(saleTransactionRepository.getDailyCompanySalesSummary(companyId, date));
    }

    public BigDecimal[] getCompanyTotalSalesSummary(Long companyId) {
        return toBigDecimalArray(saleTransactionRepository.getCompanyTotalSalesSummary(companyId));
    }

    public BigDecimal[] getMonthToDateCompanySalesSummary(Long companyId) {
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        return toBigDecimalArray(saleTransactionRepository.getSalesSummaryBetween(companyId, start, now));
    }

    public BigDecimal[] getSalesSummaryBetween(Long companyId, LocalDate start, LocalDate end) {
        return toBigDecimalArray(saleTransactionRepository.getSalesSummaryBetween(companyId, start, end));
    }

    private BigDecimal[] toBigDecimalArray(Object[] result) {
        if (result == null || result.length < 3 || result[0] == null) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        }
        return new BigDecimal[]{(BigDecimal) result[0], (BigDecimal) result[1], (BigDecimal) result[2]};
    }

    private Map<String, BigDecimal[]> toMap(List<Object[]> list) {
        return list.stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> new BigDecimal[]{(BigDecimal) obj[1], (BigDecimal) obj[2], (BigDecimal) obj[3]}
                ));
    }

    // Helper to create a new SaleTransaction
    public SaleTransaction createNew(Company company, Branch branch, Product product, SaleTransaction.ShiftSession shift, LocalDate date) {
        SaleTransaction st = new SaleTransaction();
        st.setCompany(company);
        st.setBranch(branch);
        st.setProduct(product);
        st.setShiftSession(shift);
        st.setTransactionDate(date);
        return st;
    }
}