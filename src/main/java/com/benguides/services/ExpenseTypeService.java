package com.benguides.services;

import com.benguides.dtos.ExpenseTypeDTO;
import com.benguides.models.Company;
import com.benguides.models.ExpenseType;
import com.benguides.repositories.ExpenseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseTypeService {

    private final ExpenseTypeRepository expenseTypeRepository;

    public ExpenseType createNew(Company company, String name) {
        ExpenseType type = new ExpenseType();
        type.setCompany(company);
        type.setName(name);
        return type;
    }

    public ExpenseType save(ExpenseType type) {
        return expenseTypeRepository.save(type);
    }

    public List<ExpenseTypeDTO> findByCompanyId(Long companyId) {
        return expenseTypeRepository.findByCompanyId(companyId);
    }

    public List<ExpenseType> getAllByCompany(Long companyId) {
        return expenseTypeRepository.findAllByCompanyId(companyId); // Assuming you add this method or use DTOs
    }

    public Optional<ExpenseType> findByCompanyAndName(Long companyId, String name) {
        return expenseTypeRepository.findByCompanyIdAndName(companyId, name);
    }

    public void delete(Long id) {
        expenseTypeRepository.deleteById(id);
    }
}