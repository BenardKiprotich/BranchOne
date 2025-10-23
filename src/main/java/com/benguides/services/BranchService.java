package com.benguides.services;

import com.benguides.dtos.CompanyDTO;
import com.benguides.models.Branch;
import com.benguides.models.Company;
import com.benguides.repositories.BranchRepository;
import com.benguides.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public Branch createBranch(Branch branch, Company company) {
        branch.setCompany(company);
        return branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranchesByCompany(Long companyId) {
        return branchRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<Branch> getActiveBranchesByCompany(Long companyId) {
        return branchRepository.findActiveBranchesByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public Optional<CompanyDTO> getCompanyStats(Long companyId) {
        log.info("Fetching company stats for company ID: {}", companyId);

        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            log.warn("Company not found with ID: {}", companyId);
            return Optional.empty();
        }

        Company company = companyOpt.get();

        long totalBranches = branchRepository.countByCompanyId(companyId);
        long activeBranches = branchRepository.countActiveBranchesByCompany(companyId);

        log.info("Company stats - Total branches: {}, Active branches: {}", totalBranches, activeBranches);

        CompanyDTO companyDTO = new CompanyDTO();
        companyDTO.setId(company.getId());
        companyDTO.setName(company.getName());
        companyDTO.setAddress(company.getAddress());
        companyDTO.setPhone(company.getPhone());
        companyDTO.setEmail(company.getEmail());
        companyDTO.setSubscriptionPlan(company.getSubscriptionPlan());
        companyDTO.setActive(company.isActive());
        companyDTO.setTotalBranches((int) totalBranches);
        companyDTO.setActiveBranches((int) activeBranches);

        return Optional.of(companyDTO);
    }

    @Transactional(readOnly = true)
    public Optional<Branch> getBranchById(Long id) {
        return branchRepository.findById(id);
    }

    @Transactional
    public Branch updateBranch(Branch branch) {
        return branchRepository.save(branch);
    }

    @Transactional
    public void deactivateBranch(Long id) {
        branchRepository.findById(id).ifPresent(branch -> {
            branch.setActive(false);
            branchRepository.save(branch);
        });
    }
    @Transactional
    public void deleteBranch(Long id) {
        branchRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean branchNameOrCodeExists(Long companyId, String name, String code) {
        return branchRepository.existsByCompanyIdAndName(companyId, name) ||
                branchRepository.existsByCompanyIdAndCode(companyId, code);
    }

    @Transactional(readOnly = true)
    public boolean canUpdateBranch(Long branchId, String name, String code, Long companyId) {
        Optional<Branch> existingBranch = branchRepository.findByCompanyIdAndNameOrCodeExcludingId(companyId, name, code, branchId);
        return existingBranch.isPresent();
    }
}
