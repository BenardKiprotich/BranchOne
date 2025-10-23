package com.benguides.services;

import com.benguides.dtos.CompanyCreateRequest;
import com.benguides.dtos.CompanyUpdateRequest;
import com.benguides.models.Company;
import com.benguides.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;

    public Company createCompany(@Valid CompanyCreateRequest request) {
        // Check if company name already exists
        if (companyRepository.existsByName(request.getName())) {
            throw new RuntimeException("Company with name '" + request.getName() + "' already exists");
        }

        // Check if company email already exists
        if (companyRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Company with email '" + request.getEmail() + "' already exists");
        }

        Company company = new Company();
        company.setName(request.getName());
        company.setAddress(request.getAddress());
        company.setPhone(request.getCompanyPhone());
        company.setEmail(request.getEmail());
        company.setActive(true);

        return companyRepository.save(company);
    }

    public Company createCompany(@Valid Company company) {
        if (companyRepository.existsByName(company.getName())) {
            throw new RuntimeException("Company with name '" + company.getName() + "' already exists");
        }
        if (companyRepository.existsByEmail(company.getEmail())) {
            throw new RuntimeException("Company with email '" + company.getEmail() + "' already exists");
        }
        return companyRepository.save(company);
    }

    public Company updateCompany(Long id, @Valid CompanyUpdateRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));

        // Check if new name conflicts with existing companies (excluding current)
        if (!company.getName().equals(request.getName()) &&
                companyRepository.existsByName(request.getName())) {
            throw new RuntimeException("Company with name '" + request.getName() + "' already exists");
        }

        // Check if new email conflicts with existing companies (excluding current)
        if (!company.getEmail().equals(request.getEmail()) &&
                companyRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Company with email '" + request.getEmail() + "' already exists");
        }

        company.setName(request.getName());
        company.setAddress(request.getAddress());
        company.setPhone(request.getPhone());
        company.setEmail(request.getEmail());

        return companyRepository.save(company);
    }

    public Company updateCompany(Company company) {
        return companyRepository.save(company);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public List<Company> getActiveCompanies() {
        return companyRepository.findByIsActiveTrue();
    }

    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public Optional<Company> getCompanyByName(String name) {
        return companyRepository.findByName(name);
    }

    public void deactivateCompany(Long id) {
        companyRepository.findById(id).ifPresent(company -> {
            company.setActive(false);
            companyRepository.save(company);
        });
    }

    public boolean companyExists(String name) {
        return companyRepository.existsByName(name);
    }

    public boolean companyEmailExists(String email) {
        return companyRepository.existsByEmail(email);
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
    }
}