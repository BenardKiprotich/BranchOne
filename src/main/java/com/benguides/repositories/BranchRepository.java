package com.benguides.repositories;

import com.benguides.models.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByCompanyId(Long companyId);

    @Query("SELECT b FROM Branch b WHERE b.company.id = :companyId AND b.isActive = true")
    List<Branch> findActiveBranchesByCompany(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(b) FROM Branch b WHERE b.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(b) FROM Branch b WHERE b.company.id = :companyId AND b.isActive = true")
    long countActiveBranchesByCompany(@Param("companyId") Long companyId);

    boolean existsByCompanyIdAndName(Long companyId, String name);
    boolean existsByCompanyIdAndCode(Long companyId, String code);

    @Query("SELECT b FROM Branch b WHERE b.company.id = :companyId AND (b.name = :name OR b.code = :code) AND b.id != :branchId")
    Optional<Branch> findByCompanyIdAndNameOrCodeExcludingId(@Param("companyId") Long companyId, @Param("name") String name, @Param("code") String code, @Param("branchId") Long branchId);
}
